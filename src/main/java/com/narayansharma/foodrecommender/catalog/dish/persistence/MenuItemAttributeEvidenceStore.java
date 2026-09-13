package com.narayansharma.foodrecommender.catalog.dish.persistence;

import com.narayansharma.foodrecommender.catalog.dish.extraction.DishAttributeCandidate;
import com.narayansharma.foodrecommender.catalog.dish.extraction.DishAttributeExtractor;
import com.narayansharma.foodrecommender.catalog.dish.extraction.DishAttributeType;
import com.narayansharma.foodrecommender.catalog.dish.extraction.MenuItemText;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemAttributeEvidenceStore {
	private final JdbcTemplate jdbcTemplate;
	private final DishAttributeExtractor extractor;
	private final Clock clock;
	private final String extractionMethod;

	public MenuItemAttributeEvidenceStore(
			JdbcTemplate jdbcTemplate,
			DishAttributeExtractor extractor,
			Clock clock,
			@Value("${dish.attributes.extraction-method:rule-lexicon-v1}") String extractionMethod) {
		if (extractionMethod == null
				|| !extractionMethod.matches("[a-z][a-z0-9_-]{0,99}")) {
			throw new IllegalArgumentException("Dish attribute extraction method is invalid");
		}
		this.jdbcTemplate = jdbcTemplate;
		this.extractor = extractor;
		this.clock = clock;
		this.extractionMethod = extractionMethod;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(UUID menuExtractionId, UUID menuItemId, MenuItemText itemText) {
		if (menuExtractionId == null || menuItemId == null || itemText == null) {
			throw new IllegalArgumentException("Menu extraction, item, and text are required");
		}
		requireSameMenuVersion(menuExtractionId, menuItemId);
		Instant createdAt = Instant.now(clock);
		for (DishAttributeCandidate candidate : extractor.extract(itemText)) {
			if (candidate.type() == DishAttributeType.INGREDIENT) {
				insertIngredient(menuExtractionId, menuItemId, candidate, createdAt);
			} else {
				insertTrait(menuExtractionId, menuItemId, candidate, createdAt);
			}
		}
	}

	private void requireSameMenuVersion(UUID extractionId, UUID itemId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM menu_extractions extraction
				JOIN menu_sections section
				  ON section.menu_version_id = extraction.menu_version_id
				JOIN menu_items item
				  ON item.menu_section_id = section.id
				WHERE extraction.id = ? AND item.id = ?
				""", Integer.class, extractionId, itemId);
		if (count == null || count != 1) {
			throw new IllegalArgumentException("Menu extraction and item must share a menu version");
		}
	}

	private void insertIngredient(
			UUID extractionId,
			UUID itemId,
			DishAttributeCandidate candidate,
			Instant createdAt) {
		UUID ingredientId = requireVocabularyId("ingredients", "ingredient_key", candidate.attributeKey());
		jdbcTemplate.update("""
				INSERT INTO menu_item_ingredient_evidence (
				    id, menu_extraction_id, menu_item_id, ingredient_id,
				    assertion, evidence_type, confidence, source_text,
				    extraction_method, created_at
				) VALUES (?, ?, ?, ?, 'PRESENT', ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				extractionId,
				itemId,
				ingredientId,
				candidate.evidenceType().name(),
				BigDecimal.valueOf(candidate.confidence()),
				candidate.matchedText(),
				extractionMethod,
				Timestamp.from(createdAt));
	}

	private void insertTrait(
			UUID extractionId,
			UUID itemId,
			DishAttributeCandidate candidate,
			Instant createdAt) {
		UUID traitId = requireVocabularyId("dish_traits", "trait_key", candidate.attributeKey());
		jdbcTemplate.update("""
				INSERT INTO menu_item_trait_evidence (
				    id, menu_extraction_id, menu_item_id, trait_id,
				    assertion, evidence_type, confidence, source_text,
				    extraction_method, created_at
				) VALUES (?, ?, ?, ?, 'PRESENT', ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				extractionId,
				itemId,
				traitId,
				candidate.evidenceType().name(),
				BigDecimal.valueOf(candidate.confidence()),
				candidate.matchedText(),
				extractionMethod,
				Timestamp.from(createdAt));
	}

	private UUID requireVocabularyId(String table, String keyColumn, String key) {
		List<UUID> ids = jdbcTemplate.query(
				"SELECT id FROM " + table + " WHERE " + keyColumn + " = ?",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				key);
		return ids.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing dish vocabulary key: " + key));
	}
}
