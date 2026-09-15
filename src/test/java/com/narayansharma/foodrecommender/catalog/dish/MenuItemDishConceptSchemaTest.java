package com.narayansharma.foodrecommender.catalog.dish;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MenuItemDishConceptSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void allowsManyMenuItemsToShareOneConceptAndOthersToRemainUnmatched() {
		UUID dishConceptId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, created_at, updated_at
				) VALUES (?, 'lobster_roll_schema_test', 'Lobster Roll', 'lobster roll', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishConceptId);
		UUID sectionId = insertMenuSection();
		UUID matchedItemId = UUID.randomUUID();
		UUID secondMatchedItemId = UUID.randomUUID();
		UUID unmatchedItemId = UUID.randomUUID();
		insertMenuItem(matchedItemId, sectionId, "Warm Lobster Roll", 0, dishConceptId);
		insertMenuItem(secondMatchedItemId, sectionId, "Cold Lobster Roll", 1, dishConceptId);
		insertMenuItem(unmatchedItemId, sectionId, "Chef Special", 2, null);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_items WHERE dish_concept_id = ?",
				Integer.class,
				dishConceptId)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT dish_concept_id FROM menu_items WHERE id = ?",
				UUID.class,
				unmatchedItemId)).isNull();
	}

	private UUID insertMenuSection() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Harbor Cafe', 'harbor cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Main Street', 'Boston', 'MA', 'US',
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
		jdbcTemplate.update("""
				INSERT INTO menu_sections (id, menu_version_id, display_name, display_order)
				VALUES (?, ?, 'Sandwiches', 0)
				""", sectionId, versionId);
		return sectionId;
	}

	private void insertMenuItem(
			UUID itemId,
			UUID sectionId,
			String name,
			int order,
			UUID dishConceptId) {
		jdbcTemplate.update("""
				INSERT INTO menu_items (
				    id, menu_section_id, display_name, display_order, dish_concept_id
				) VALUES (?, ?, ?, ?, ?)
				""", itemId, sectionId, name, order, dishConceptId);
	}
}
