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
	private RatingModerationService moderationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsCurrentFeedbackAndItsFirstRevision() {
		UUID userId = insertUser();
		MenuFixture menu = insertMenuItemWithDish();

		UUID ratingId = ratingService.create(
				userId,
				new SaveRatingRequest(
						menu.itemId(), 5, true, "  Great crispy crust  ", List.of("crispy", "crispy", "savory")));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT dish_concept_id FROM ratings WHERE id = ?", UUID.class, ratingId))
				.isEqualTo(menu.dishId());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT original_text FROM rating_comments WHERE rating_id = ?", String.class, ratingId))
				.isEqualTo("Great crispy crust");
		assertThat(count("rating_tags", "rating_id", ratingId)).isEqualTo(2);
		assertThat(count("rating_revisions", "rating_id", ratingId)).isEqualTo(1);
		assertThat(count("feedback_change_events", "rating_id", ratingId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForList("""
				SELECT signal.trait_key
				FROM rating_trait_signals signal
				JOIN rating_revisions revision ON revision.id = signal.revision_id
				WHERE revision.rating_id = ?
				ORDER BY signal.trait_key
				""", String.class, ratingId)).containsExactly("crispy");
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

	@Test
	void updatesFeedbackWithoutErasingHistory() {
		UUID userId = insertUser();
		UUID itemId = insertMenuItemWithDish().itemId();
		UUID ratingId = ratingService.create(
				userId, new SaveRatingRequest(itemId, 3, false, "Okay", List.of("mild")));

		ratingService.update(
				userId, ratingId, new SaveRatingRequest(itemId, 5, true, "Excellent", List.of("savory")));

		RatingView current = ratingQueryService.get(userId, ratingId);
		assertThat(current.score()).isEqualTo(5);
		assertThat(current.comment()).isEqualTo("Excellent");
		assertThat(ratingQueryService.history(userId, ratingId))
				.extracting(RatingRevisionView::changeType)
				.containsExactly("UPDATED", "CREATED");
		assertThat(count("feedback_change_events", "rating_id", ratingId)).isEqualTo(2);
	}

	@Test
	void softDeletesFeedbackAndKeepsItsAuditHistory() {
		UUID userId = insertUser();
		UUID itemId = insertMenuItemWithDish().itemId();
		UUID ratingId = ratingService.create(
				userId, new SaveRatingRequest(itemId, 2, false, "Not for me", List.of("salty")));

		ratingService.delete(userId, ratingId);

		assertThatThrownBy(() -> ratingQueryService.get(userId, ratingId))
				.isInstanceOf(ApiException.class);
		assertThat(ratingQueryService.history(userId, ratingId))
				.extracting(RatingRevisionView::changeType)
				.containsExactly("DELETED", "CREATED");
		assertThat(count("rating_comments", "rating_id", ratingId)).isZero();
		assertThat(count("feedback_change_events", "rating_id", ratingId)).isEqualTo(2);
	}

	@Test
	void opensAModerationCaseForOwnedFeedback() {
		UUID userId = insertUser();
		UUID ratingId = ratingService.create(
				userId,
				new SaveRatingRequest(insertMenuItemWithDish().itemId(), 3, null, "Fine", List.of()));

		ReportRatingResponse report = moderationService.report(
				userId, ratingId, new ReportRatingRequest("OTHER", "Needs review"));

		assertThat(report.status()).isEqualTo("OPEN");
		assertThat(count("rating_moderation_cases", "rating_id", ratingId)).isEqualTo(1);
	}

	@Test
	void allowsANewRatingAfterThePreviousOneIsDeleted() {
		UUID userId = insertUser();
		UUID itemId = insertMenuItemWithDish().itemId();
		SaveRatingRequest request = new SaveRatingRequest(itemId, 4, true, null, List.of());
		UUID deletedRatingId = ratingService.create(userId, request);
		ratingService.delete(userId, deletedRatingId);

		UUID newRatingId = ratingService.create(userId, request);

		assertThat(newRatingId).isNotEqualTo(deletedRatingId);
		assertThat(ratingQueryService.get(userId, newRatingId).score()).isEqualTo(4);
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
