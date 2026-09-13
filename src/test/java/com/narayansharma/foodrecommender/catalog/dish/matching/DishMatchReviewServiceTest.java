package com.narayansharma.foodrecommender.catalog.dish.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DishMatchReviewServiceTest {
	@Autowired
	private DishMatchReviewService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void queuesUncertainMatchesOnceAndLinksAnApprovedDish() {
		UUID menuItemId = insertMenuItem();
		UUID dishId = insertDish();
		UUID competingDishId = insertDish();

		StoredDishMatchReview queued = service.queueUncertain(
				menuItemId,
				dishId,
				0.72,
				"name-similarity-v1");
		StoredDishMatchReview repeated = service.queueUncertain(
				menuItemId,
				dishId,
				0.72,
				"name-similarity-v1");
		service.queueUncertain(menuItemId, competingDishId, 0.68, "name-similarity-v1");

		assertThat(queued.created()).isTrue();
		assertThat(repeated.created()).isFalse();
		assertThat(repeated.id()).isEqualTo(queued.id());
		assertThat(service.pending(10)).extracting(StoredDishMatchReview::id).contains(queued.id());

		StoredDishMatchReview approved = service.resolve(
				queued.id(),
				DishMatchReviewDecision.APPROVE,
				"admin:owner",
				"Names describe the same dish");

		assertThat(approved.status()).isEqualTo("APPROVED");
		assertThat(approved.resolvedAt()).isNotNull();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT dish_concept_id FROM menu_items WHERE id = ?",
				UUID.class,
				menuItemId)).isEqualTo(dishId);
		assertThat(service.pending(10))
				.noneMatch(review -> review.menuItemId().equals(menuItemId));
	}

	@Test
	void rejectsScoresOutsideTheReviewBand() {
		UUID menuItemId = insertMenuItem();
		UUID dishId = insertDish();

		assertThatThrownBy(() -> service.queueUncertain(
				menuItemId,
				dishId,
				0.95,
				"name-similarity-v1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Only uncertain");
	}

	private UUID insertDish() {
		UUID dishId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, created_at, updated_at
				) VALUES (?, ?, 'Clam Chowder', 'clam chowder', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishId, "match_test_" + dishId);
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
				VALUES (?, 'Match Kitchen', 'match kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Match Way', 'Boston', 'MA', 'US',
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
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/match-menu',
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
				VALUES (?, ?, 'New England Chowder', 0)
				""", itemId, sectionId);
		return itemId;
	}
}
