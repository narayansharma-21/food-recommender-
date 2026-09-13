package com.narayansharma.foodrecommender.catalog.dish;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DishMatchReviewSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void requiresAttributableResolutionForAQueuedMatch() {
		UUID menuItemId = insertMenuItem();
		UUID dishId = insertDish();
		UUID reviewId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_match_reviews (
				    id, menu_item_id, suggested_dish_concept_id, confidence,
				    match_method, status, created_at
				) VALUES (?, ?, ?, 0.72, 'name-similarity-v1', 'PENDING', CURRENT_TIMESTAMP)
				""", reviewId, menuItemId, dishId);

		assertThatThrownBy(() -> jdbcTemplate.update(
				"UPDATE dish_match_reviews SET status = 'APPROVED' WHERE id = ?",
				reviewId)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private UUID insertDish() {
		UUID dishId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, created_at, updated_at
				) VALUES (?, ?, 'Review Dish', 'review dish', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishId, "review_dish_" + dishId);
		return dishId;
	}

	private UUID insertMenuItem() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Review Kitchen', 'review kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Review Way', 'Boston', 'MA', 'US',
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
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/review-menu',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", sourceId, menuId);
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", versionId, menuId, sourceId);
		jdbcTemplate.update(
				"INSERT INTO menu_sections (id, menu_version_id, display_name, display_order) VALUES (?, ?, 'Entrees', 0)",
				sectionId,
				versionId);
		jdbcTemplate.update("""
				INSERT INTO menu_items (id, menu_section_id, display_name, display_order)
				VALUES (?, ?, 'Review Item', 0)
				""", itemId, sectionId);
		return itemId;
	}
}
