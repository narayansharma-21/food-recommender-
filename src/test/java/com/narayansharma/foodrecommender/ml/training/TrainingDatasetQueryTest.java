package com.narayansharma.foodrecommender.ml.training;

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
class TrainingDatasetQueryTest {
	@Autowired
	private TrainingDatasetQuery query;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void attributesARatingToOnlyTheMostRecentEarlierImpression() {
		Fixture fixture = insertFixture();
		Instant firstTime = Instant.parse("2026-01-01T10:00:00Z");
		Instant secondTime = Instant.parse("2026-01-01T11:00:00Z");
		Instant ratedAt = Instant.parse("2026-01-01T12:00:00Z");
		insertImpression(fixture, UUID.randomUUID(), firstTime);
		UUID expectedResultId = UUID.randomUUID();
		insertImpression(fixture, expectedResultId, secondTime);
		UUID ratingId = insertRating(fixture, ratedAt);

		assertThat(query.examples(
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z")))
				.singleElement()
				.satisfies(example -> {
					assertThat(example.recommendationResultId()).isEqualTo(expectedResultId);
					assertThat(example.ratingId()).isEqualTo(ratingId);
					assertThat(example.recommendedAt()).isEqualTo(secondTime);
					assertThat(example.ratingScore()).isEqualTo(4);
					assertThat(example.personalPreference()).isEqualByComparingTo("0.250000");
				});
		assertThat(query.examples(Instant.parse("2026-01-01T00:00:00Z"), ratedAt)).isEmpty();
	}

	private Fixture insertFixture() {
		UUID userId = UUID.randomUUID();
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'ML Cafe', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId, "ml cafe " + restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Model Way', 'Boston', 'MA', 'US',
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
		jdbcTemplate.update("""
				INSERT INTO menu_items (id, menu_section_id, display_name, display_order)
				VALUES (?, ?, 'Model Meal', 0)
				""", itemId, sectionId);
		return new Fixture(userId, restaurantId, versionId, itemId);
	}

	private void insertImpression(Fixture fixture, UUID resultId, Instant generatedAt) {
		UUID requestId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO recommendation_requests (
				    id, user_id, restaurant_id, menu_version_id, mode,
				    algorithm_version, feature_version, generated_at
				) VALUES (?, ?, ?, ?, 'SAFE_BET', 'weighted-rules-v1', 'taste-profile-v1', ?)
				""", requestId, fixture.userId(), fixture.restaurantId(), fixture.menuVersionId(),
				Timestamp.from(generatedAt));
		jdbcTemplate.update("""
				INSERT INTO recommendation_results (
				    id, request_id, menu_item_id, result_rank, score, confidence
				) VALUES (?, ?, ?, 1, 0.4, 'MEDIUM')
				""", resultId, requestId, fixture.menuItemId());
		jdbcTemplate.update("""
				INSERT INTO recommendation_result_features (
				    result_id, personal_preference, popularity, taste_evidence_count,
				    popularity_rating_count, previously_rated
				) VALUES (?, 0.25, 0.75, 2, 4, FALSE)
				""", resultId);
	}

	private UUID insertRating(Fixture fixture, Instant ratedAt) {
		UUID ratingId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO ratings (
				    id, user_id, menu_item_id, score, would_order_again, created_at, updated_at
				) VALUES (?, ?, ?, 4, TRUE, ?, ?)
				""", ratingId, fixture.userId(), fixture.menuItemId(),
				Timestamp.from(ratedAt), Timestamp.from(ratedAt));
		jdbcTemplate.update("""
				INSERT INTO rating_revisions (
				    id, rating_id, revision_number, change_type, score, would_order_again, created_at
				) VALUES (?, ?, 1, 'CREATED', 4, TRUE, ?)
				""", UUID.randomUUID(), ratingId, Timestamp.from(ratedAt));
		return ratingId;
	}

	private record Fixture(UUID userId, UUID restaurantId, UUID menuVersionId, UUID menuItemId) {
	}
}
