package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecommendationCandidateServiceTest {
	@Autowired
	private RecommendationCandidateService candidateService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void selectsItemsOnlyFromTheLatestMenuVersion() {
		UUID userId = insertUser();
		MenuFixture fixture = insertVersionedMenu();

		assertThat(candidateService.safeCandidates(userId, fixture.restaurantId()))
				.extracting(RecommendationCandidate::displayName)
				.containsExactly("Current Item");
	}

	@Test
	void failsClosedForAnUnsupportedDietaryRestriction() {
		UUID userId = insertUser();
		MenuFixture fixture = insertVersionedMenu();
		jdbcTemplate.update("""
				INSERT INTO user_restrictions (
				    id, user_id, restriction_type, restriction_key,
				    display_name, active, created_at, updated_at
				) VALUES (?, ?, 'DIETARY', 'kosher', 'Kosher', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), userId);

		assertThat(candidateService.safeCandidates(userId, fixture.restaurantId())).isEmpty();
	}

	private UUID insertUser() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		return userId;
	}

	private MenuFixture insertVersionedMenu() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Ranking Cafe', 'ranking cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Rank Way', 'Boston', 'MA', 'US',
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
		insertVersion(menuId, sourceId, 1, Instant.parse("2026-01-01T00:00:00Z"), "Old Item");
		UUID currentVersionId = insertVersion(
				menuId, sourceId, 2, Instant.parse("2026-02-01T00:00:00Z"), "Current Item");
		return new MenuFixture(restaurantId, currentVersionId);
	}

	private UUID insertVersion(UUID menuId, UUID sourceId, int number, Instant capturedAt, String itemName) {
		UUID versionId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
				""", versionId, menuId, sourceId, number, Timestamp.from(capturedAt));
		jdbcTemplate.update("""
				INSERT INTO menu_sections (id, menu_version_id, display_name, display_order)
				VALUES (?, ?, 'Entrees', 0)
				""", sectionId, versionId);
		jdbcTemplate.update("""
				INSERT INTO menu_items (id, menu_section_id, display_name, display_order)
				VALUES (?, ?, ?, 0)
				""", UUID.randomUUID(), sectionId, itemName);
		return versionId;
	}

	private record MenuFixture(UUID restaurantId, UUID currentVersionId) {
	}
}
