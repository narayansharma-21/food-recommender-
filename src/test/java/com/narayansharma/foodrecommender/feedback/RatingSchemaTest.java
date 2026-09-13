package com.narayansharma.foodrecommender.feedback;

import static org.assertj.core.api.Assertions.assertThat;
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
class RatingSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesFeedbackAgainstTheExactMenuItem() {
		UUID userId = insertUser();
		UUID itemId = insertMenuItem();
		UUID ratingId = insertRating(userId, itemId, 5);
		jdbcTemplate.update("""
				INSERT INTO rating_comments (rating_id, original_text, created_at, updated_at)
				VALUES (?, 'Excellent crispy crust', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", ratingId);
		jdbcTemplate.update("""
				INSERT INTO rating_tags (rating_id, tag_key, created_at)
				VALUES (?, 'crispy', CURRENT_TIMESTAMP)
				""", ratingId);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT score FROM ratings WHERE id = ?", Integer.class, ratingId)).isEqualTo(5);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT original_text FROM rating_comments WHERE rating_id = ?", String.class, ratingId))
				.isEqualTo("Excellent crispy crust");
	}

	@Test
	void rejectsScoresOutsideOneToFive() {
		UUID userId = insertUser();
		UUID itemId = insertMenuItem();

		assertThatThrownBy(() -> insertRating(userId, itemId, 6))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void keepsImmutableRatingRevisions() {
		UUID ratingId = insertRating(insertUser(), insertMenuItem(), 4);
		UUID revisionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO rating_revisions (
				    id, rating_id, revision_number, change_type, score,
				    would_order_again, original_comment, created_at
				) VALUES (?, ?, 1, 'CREATED', 4, TRUE, 'Very good', CURRENT_TIMESTAMP)
				""", revisionId, ratingId);
		jdbcTemplate.update("""
				INSERT INTO rating_revision_tags (revision_id, tag_key)
				VALUES (?, 'savory')
				""", revisionId);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT original_comment FROM rating_revisions WHERE id = ?", String.class, revisionId))
				.isEqualTo("Very good");
	}

	private UUID insertUser() {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id);
		return id;
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
				VALUES (?, 'Test Cafe', 'test cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Main St', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", locationId, restaurantId);
		jdbcTemplate.update("""
				INSERT INTO menus (id, restaurant_location_id, menu_key, display_name, created_at, updated_at)
				VALUES (?, ?, 'main', 'Main', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", menuId, locationId);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (id, menu_id, source_type, origin_url, created_at, updated_at)
				VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", sourceId, menuId);
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
				VALUES (?, ?, 'Test Dish', 0)
				""", itemId, sectionId);
		return itemId;
	}

	private UUID insertRating(UUID userId, UUID itemId, int score) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO ratings (
				    id, user_id, menu_item_id, score, would_order_again, created_at, updated_at
				) VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, userId, itemId, score);
		return id;
	}
}
