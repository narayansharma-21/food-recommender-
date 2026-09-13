package com.narayansharma.foodrecommender.catalog.dish.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.persistence.OriginalMenuExtractionStore;
import com.narayansharma.foodrecommender.menu.extraction.persistence.StoredOriginalExtraction;
import com.narayansharma.foodrecommender.menu.extraction.structured.MenuTextExtractor;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AllergyEvidencePolicyTest {
	@Autowired
	private AllergyEvidencePolicy policy;

	@Autowired
	private OriginalMenuExtractionStore extractionStore;

	@Autowired
	private MenuTextExtractor textExtractor;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void usesOnlyDeclaredOrCorrectedEvidenceForAllergyDecisions() {
		UUID menuVersionId = insertMenuVersion();
		OcrResult ocr = new OcrResult(
				"test_ocr",
				"1",
				List.of(new OcrPage(1, "ENTREES\nLobster Roll  smoked bacon  $29.00", 0.9)));
		StoredOriginalExtraction extraction = extractionStore.store(
				menuVersionId,
				ocr,
				textExtractor.extractWithEvidence(ocr),
				"rules-v1");
		UUID menuItemId = jdbcTemplate.queryForObject("""
				SELECT item.id
				FROM menu_items item
				JOIN menu_sections section ON section.id = item.menu_section_id
				WHERE section.menu_version_id = ?
				""", UUID.class, menuVersionId);
		insertDeclaredAbsence(extraction.id(), menuItemId, "20000000-0000-0000-0000-000000000001");
		insertCorrectedAbsence(extraction.id(), menuItemId, "20000000-0000-0000-0000-000000000004");

		assertThat(policy.assess(menuItemId, Set.of("lobster")))
				.isEqualTo(AllergyEvidenceAssessment.CONTAINS_ALLERGEN);
		assertThat(policy.assess(menuItemId, Set.of("clam")))
				.isEqualTo(AllergyEvidenceAssessment.CONFIRMED_ABSENT);
		assertThat(policy.assess(menuItemId, Set.of("shrimp")))
				.isEqualTo(AllergyEvidenceAssessment.CONFIRMED_ABSENT);
		assertThat(policy.assess(menuItemId, Set.of("pork")))
				.isEqualTo(AllergyEvidenceAssessment.UNKNOWN);
		assertThat(policy.assess(menuItemId, Set.of("clam", "pork")))
				.isEqualTo(AllergyEvidenceAssessment.UNKNOWN);
	}

	private void insertDeclaredAbsence(UUID extractionId, UUID menuItemId, String ingredientId) {
		jdbcTemplate.update("""
				INSERT INTO menu_item_ingredient_evidence (
				    id, menu_extraction_id, menu_item_id, ingredient_id,
				    assertion, evidence_type, confidence, source_text,
				    extraction_method, created_at
				) VALUES (?, ?, ?, ?, 'ABSENT', 'DECLARED', 1, 'no clam',
				          'test-declaration', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), extractionId, menuItemId, UUID.fromString(ingredientId));
	}

	private void insertCorrectedAbsence(UUID extractionId, UUID menuItemId, String ingredientId) {
		jdbcTemplate.update("""
				INSERT INTO menu_item_ingredient_evidence (
				    id, menu_extraction_id, menu_item_id, ingredient_id,
				    assertion, evidence_type, confidence, source_text,
				    extraction_method, created_by_reference, created_at
				) VALUES (?, ?, ?, ?, 'ABSENT', 'USER_CORRECTED', 1, 'confirmed no shrimp',
				          'human-correction', 'admin:test', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), extractionId, menuItemId, UUID.fromString(ingredientId));
	}

	private UUID insertMenuVersion() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Safety Kitchen', 'safety kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Safety Way', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", locationId, restaurantId);
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, 'main', 'Main Menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", menuId, locationId);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, origin_url, created_at, updated_at
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/safety-menu',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", sourceId, menuId);
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", versionId, menuId, sourceId);
		return versionId;
	}
}
