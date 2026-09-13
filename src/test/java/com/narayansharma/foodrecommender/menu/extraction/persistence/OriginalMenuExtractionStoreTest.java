package com.narayansharma.foodrecommender.menu.extraction.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.structured.MenuTextExtractor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OriginalMenuExtractionStoreTest {
	@Autowired
	private OriginalMenuExtractionStore store;

	@Autowired
	private MenuTextExtractor textExtractor;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesTheOriginalOcrStructureAndEvidenceWithoutOverwritingIt() {
		UUID menuVersionId = insertMenuVersion();
		OcrResult ocrResult = new OcrResult(
				"test_ocr",
				"1",
				List.of(new OcrPage(1, "ENTREES\nLobster Roll  Buttered roll  $29.00\nAdd fries $4.00", 0.9)));
		var extraction = textExtractor.extractWithEvidence(ocrResult);

		StoredOriginalExtraction first = store.store(menuVersionId, ocrResult, extraction, "rules-v1");
		StoredOriginalExtraction retry = store.store(menuVersionId, ocrResult, extraction, "rules-v1");

		assertThat(first.created()).isTrue();
		assertThat(retry.created()).isFalse();
		assertThat(retry.id()).isEqualTo(first.id());
		var row = jdbcTemplate.queryForMap("""
				SELECT ocr_result_json, structured_result_json, field_evidence_json
				FROM menu_extractions
				WHERE id = ?
				""", first.id());
		assertThat(row.get("ocr_result_json").toString()).contains("Lobster Roll", "test_ocr");
		assertThat(row.get("structured_result_json").toString()).contains("Lobster Roll", "29.00");
		assertThat(row.get("field_evidence_json").toString()).contains("sections[0].items[0].name", "OCR_TEXT");
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_extractions WHERE menu_version_id = ?",
				Integer.class,
				menuVersionId);
		assertThat(count).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_sections WHERE menu_version_id = ?",
				Integer.class,
				menuVersionId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM menu_items item
				JOIN menu_sections section ON section.id = item.menu_section_id
				WHERE section.menu_version_id = ? AND item.display_name = 'Lobster Roll'
				""", Integer.class, menuVersionId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM menu_item_modifiers modifier
				JOIN menu_items item ON item.id = modifier.menu_item_id
				JOIN menu_sections section ON section.id = item.menu_section_id
				WHERE section.menu_version_id = ? AND modifier.display_name = 'Add fries'
				""", Integer.class, menuVersionId)).isEqualTo(1);
	}

	private UUID insertMenuVersion() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Harbor Kitchen', 'harbor kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Harbor Street', 'Boston', 'MA', 'US',
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
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", sourceId, menuId);
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", versionId, menuId, sourceId);
		return versionId;
	}
}
