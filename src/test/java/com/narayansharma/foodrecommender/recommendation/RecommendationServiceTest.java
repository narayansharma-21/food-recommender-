package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecommendationServiceTest {
	@Autowired
	private RecommendationService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void returnsAndRecordsAColdStartRanking() {
		UUID userId = insertUser();
		MenuFixture menu = insertMenu("First Item", "Second Item");

		RecommendationResponse response = service.recommend(
				userId, menu.restaurantId(), new RecommendationRequest(RecommendationMode.SAFE_BET, 1));

		assertThat(response.algorithmVersion()).isEqualTo("weighted-rules-v1");
		assertThat(response.featureVersion()).isEqualTo("no-profile-v1");
		assertThat(response.menuVersionId()).isEqualTo(menu.menuVersionId());
		assertThat(response.items()).singleElement().satisfies(item -> {
			assertThat(item.rank()).isEqualTo(1);
			assertThat(item.menuItemId()).isEqualTo(menu.firstItemId());
			assertThat(item.score()).isEqualByComparingTo(new BigDecimal("0.187500"));
			assertThat(item.confidence()).isEqualTo("LOW");
			assertThat(item.reasons()).containsExactly("Available on the restaurant's current menu.");
		});
		assertThat(count("recommendation_requests")).isEqualTo(1);
		assertThat(count("recommendation_results")).isEqualTo(1);
		assertThat(count("recommendation_reasons")).isEqualTo(1);
	}

	@Test
	void recordsAnEmptyResultWhenRestrictionsRemoveEveryCandidate() {
		UUID userId = insertUser();
		MenuFixture menu = insertMenu("Only Item");
		insertDietaryRestriction(userId, "unsupported_rule");

		RecommendationResponse response = service.recommend(
				userId, menu.restaurantId(), new RecommendationRequest(RecommendationMode.SAFE_BET, 10));

		assertThat(response.items()).isEmpty();
		assertThat(response.menuVersionId()).isEqualTo(menu.menuVersionId());
		assertThat(count("recommendation_requests")).isEqualTo(1);
		assertThat(count("recommendation_results")).isZero();
	}

	private int count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	private UUID insertUser() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		return userId;
	}

	private void insertDietaryRestriction(UUID userId, String key) {
		jdbcTemplate.update("""
				INSERT INTO user_restrictions (
				    id, user_id, restriction_type, restriction_key,
				    display_name, active, created_at, updated_at
				) VALUES (?, ?, 'DIETARY', ?, 'Unsupported', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), userId, key);
	}

	private MenuFixture insertMenu(String... itemNames) {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Test Kitchen', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId, "test kitchen " + restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Test Way', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", locationId, restaurantId);
		jdbcTemplate.update("""
				INSERT INTO menus (id, restaurant_location_id, menu_key, display_name, created_at, updated_at)
				VALUES (?, ?, 'main', 'Main', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", menuId, locationId);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (id, menu_id, source_type, origin_url, created_at, updated_at)
				VALUES (?, ?, 'OFFICIAL_HTML', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", sourceId, menuId, "https://example.com/" + sourceId);
		jdbcTemplate.update("""
				INSERT INTO menu_versions (id, menu_id, source_id, version_number, captured_at, created_at)
				VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", versionId, menuId, sourceId);
		jdbcTemplate.update("""
				INSERT INTO menu_sections (id, menu_version_id, display_name, display_order)
				VALUES (?, ?, 'Entrees', 0)
				""", sectionId, versionId);
		UUID firstItemId = null;
		for (int index = 0; index < itemNames.length; index++) {
			UUID itemId = UUID.randomUUID();
			if (index == 0) {
				firstItemId = itemId;
			}
			jdbcTemplate.update("""
					INSERT INTO menu_items (id, menu_section_id, display_name, display_order)
					VALUES (?, ?, ?, ?)
					""", itemId, sectionId, itemNames[index], index);
		}
		return new MenuFixture(restaurantId, versionId, firstItemId);
	}

	private record MenuFixture(UUID restaurantId, UUID menuVersionId, UUID firstItemId) {
	}
}
