package com.narayansharma.foodrecommender.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RatingServiceTest {
	@Autowired
	private RatingService ratingService;

	@Autowired
	private RatingQueryService ratingQueryService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsCurrentFeedbackAndItsFirstRevision() {
		UUID userId = insertUser();
		MenuFixture menu = insertMenuItemWithDish();

		UUID ratingId = ratingService.create(
				userId,
				new SaveRatingRequest(
						menu.itemId(), 5, true, "  Great crust  ", List.of("crispy", "crispy", "savory")));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT dish_concept_id FROM ratings WHERE id = ?", UUID.class, ratingId))
				.isEqualTo(menu.dishId());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT original_text FROM rating_comments WHERE rating_id = ?", String.class, ratingId))
				.isEqualTo("Great crust");
		assertThat(count("rating_tags", "rating_id", ratingId)).isEqualTo(2);
		assertThat(count("rating_revisions", "rating_id", ratingId)).isEqualTo(1);
	}

	@Test
	void rejectsASecondRatingForTheSameUserAndItem() {
		UUID userId = insertUser();
		UUID itemId = insertMenuItemWithDish().itemId();
		SaveRatingRequest request = new SaveRatingRequest(itemId, 4, true, null, List.of());
		ratingService.create(userId, request);

		assertThatThrownBy(() -> ratingService.create(userId, request))
				.isInstanceOf(ApiException.class)
				.hasMessage("A rating already exists for this menu item.");
	}

	@Test
	void readsOnlyTheOwnersRatingAndHistory() {
		UUID userId = insertUser();
		UUID ratingId = ratingService.create(
				userId,
				new SaveRatingRequest(insertMenuItemWithDish().itemId(), 4, null, "Good", List.of("savory")));

		assertThat(ratingQueryService.get(userId, ratingId).score()).isEqualTo(4);
		assertThat(ratingQueryService.history(userId, ratingId)).hasSize(1);
		assertThatThrownBy(() -> ratingQueryService.get(insertUser(), ratingId))
				.isInstanceOf(ApiException.class)
				.hasMessage("The rating was not found.");
	}

	private UUID insertUser() {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id);
		return id;
	}

	private MenuFixture insertMenuItemWithDish() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		UUID dishId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Rating Cafe', 'rating cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, created_at, updated_at
				) VALUES (?, ?, 'Test Pizza', 'test pizza', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishId, "test_pizza_" + dishId.toString().replace("-", ""));
		jdbcTemplate.update("""
				INSERT INTO menu_items (id, menu_section_id, dish_concept_id, display_name, display_order)
				VALUES (?, ?, ?, 'Test Pizza', 0)
				""", itemId, sectionId, dishId);
		return new MenuFixture(itemId, dishId);
	}

	private Integer count(String table, String idColumn, UUID id) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = ?", Integer.class, id);
	}

	private record MenuFixture(UUID itemId, UUID dishId) {
	}
}
