package com.narayansharma.foodrecommender.menu.extraction.persistence;

import com.narayansharma.foodrecommender.menu.extraction.evidence.AttributedExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.validation.ExtractedMenuSchemaValidator;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OriginalMenuExtractionStore implements MenuExtractionResultStore {
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final ExtractedMenuSchemaValidator schemaValidator;
	private final Clock clock;

	public OriginalMenuExtractionStore(
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper,
			ExtractedMenuSchemaValidator schemaValidator,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.schemaValidator = schemaValidator;
		this.clock = clock;
	}

	@Transactional
	@Override
	public StoredOriginalExtraction store(
			UUID menuVersionId,
			OcrResult ocrResult,
			AttributedExtractedMenu extraction,
			String parserVersion) {
		validate(menuVersionId, ocrResult, extraction, parserVersion);
		lockMenuVersion(menuVersionId);
		List<StoredOriginalExtraction> existing = findOriginal(menuVersionId);
		if (!existing.isEmpty()) {
			StoredOriginalExtraction original = existing.getFirst();
			return new StoredOriginalExtraction(original.id(), original.menuVersionId(), original.createdAt(), false);
		}

		Instant createdAt = Instant.now(clock);
		UUID extractionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO menu_extractions (
				    id, menu_version_id, revision_number, extraction_kind,
				    ocr_provider, ocr_provider_version, parser_version,
				    ocr_result_json, structured_result_json, field_evidence_json,
				    created_at
				) VALUES (?, ?, 1, 'ORIGINAL', ?, ?, ?, ?, ?, ?, ?)
				""",
				extractionId,
				menuVersionId,
				ocrResult.provider(),
				ocrResult.providerVersion(),
				parserVersion,
				serialize(ocrResult),
				serialize(extraction.menu()),
				serialize(extraction.fields()),
				Timestamp.from(createdAt));
		return new StoredOriginalExtraction(extractionId, menuVersionId, createdAt, true);
	}

	private void validate(
			UUID menuVersionId,
			OcrResult ocrResult,
			AttributedExtractedMenu extraction,
			String parserVersion) {
		if (menuVersionId == null || ocrResult == null || extraction == null) {
			throw new IllegalArgumentException("Menu version, OCR result, and extraction are required");
		}
		if (parserVersion == null || parserVersion.isBlank() || parserVersion.length() > 100) {
			throw new IllegalArgumentException("Parser version is invalid");
		}
		schemaValidator.validate(extraction.menu());
	}

	private void lockMenuVersion(UUID menuVersionId) {
		List<UUID> versions = jdbcTemplate.query(
				"SELECT id FROM menu_versions WHERE id = ? FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				menuVersionId);
		if (versions.isEmpty()) {
			throw new IllegalArgumentException("Unknown menu version: " + menuVersionId);
		}
	}

	private List<StoredOriginalExtraction> findOriginal(UUID menuVersionId) {
		return jdbcTemplate.query("""
				SELECT id, created_at
				FROM menu_extractions
				WHERE menu_version_id = ? AND extraction_kind = 'ORIGINAL'
				""",
				(resultSet, rowNumber) -> new StoredOriginalExtraction(
						resultSet.getObject("id", UUID.class),
						menuVersionId,
						resultSet.getTimestamp("created_at").toInstant(),
						false),
				menuVersionId);
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Menu extraction could not be serialized", exception);
		}
	}
}
