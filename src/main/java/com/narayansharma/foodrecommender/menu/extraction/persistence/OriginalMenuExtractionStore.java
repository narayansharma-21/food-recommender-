package com.narayansharma.foodrecommender.menu.extraction.persistence;

import com.narayansharma.foodrecommender.menu.extraction.evidence.AttributedExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuItem;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuSection;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedModifier;
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
		materialize(menuVersionId, extraction.menu());
		return new StoredOriginalExtraction(extractionId, menuVersionId, createdAt, true);
	}

	private void materialize(UUID menuVersionId, ExtractedMenu menu) {
		Integer existingSections = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_sections WHERE menu_version_id = ?",
				Integer.class,
				menuVersionId);
		if (existingSections == null || existingSections != 0) {
			throw new IllegalStateException("Menu version already has materialized content");
		}
		for (int sectionIndex = 0; sectionIndex < menu.sections().size(); sectionIndex++) {
			ExtractedMenuSection section = menu.sections().get(sectionIndex);
			UUID sectionId = UUID.randomUUID();
			jdbcTemplate.update("""
					INSERT INTO menu_sections (id, menu_version_id, display_name, display_order)
					VALUES (?, ?, ?, ?)
					""", sectionId, menuVersionId, section.name(), sectionIndex);
			materializeItems(sectionId, section);
		}
	}

	private void materializeItems(UUID sectionId, ExtractedMenuSection section) {
		for (int itemIndex = 0; itemIndex < section.items().size(); itemIndex++) {
			ExtractedMenuItem item = section.items().get(itemIndex);
			UUID itemId = UUID.randomUUID();
			jdbcTemplate.update("""
					INSERT INTO menu_items (
					    id, menu_section_id, display_name, description,
					    price_amount, price_currency, display_order
					) VALUES (?, ?, ?, ?, ?, ?, ?)
					""",
					itemId,
					sectionId,
					item.name(),
					item.description(),
					item.price(),
					item.currency(),
					itemIndex);
			materializeModifiers(itemId, item.modifiers());
		}
	}

	private void materializeModifiers(UUID itemId, List<ExtractedModifier> modifiers) {
		for (int modifierIndex = 0; modifierIndex < modifiers.size(); modifierIndex++) {
			ExtractedModifier modifier = modifiers.get(modifierIndex);
			jdbcTemplate.update("""
					INSERT INTO menu_item_modifiers (
					    id, menu_item_id, display_name, price_amount, display_order
					) VALUES (?, ?, ?, ?, ?)
					""",
					UUID.randomUUID(),
					itemId,
					modifier.name(),
					modifier.price(),
					modifierIndex);
		}
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
