package com.narayansharma.foodrecommender.catalog.duplicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantDuplicateReviewServiceTest {
	private static final UUID RESTAURANT_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID RESTAURANT_B = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final UUID LOCATION_A = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_B = UUID.fromString("20000000-0000-0000-0000-000000000002");

	@Autowired
	private RestaurantDuplicateReviewService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertRestaurants() {
		insertRestaurant(RESTAURANT_A, "Cafe Luna");
		insertRestaurant(RESTAURANT_B, "Cafe Luna Duplicate");
		insertLocation(LOCATION_A, RESTAURANT_A, "10 Main Street");
		insertLocation(LOCATION_B, RESTAURANT_B, "10 Main St");
	}

	@Test
	void queuesEachLocationPairOnlyOnce() {
		UUID first = service.suggest(LOCATION_A, LOCATION_B, 0.82, List.of("similar name", "same address"));
		UUID second = service.suggest(LOCATION_B, LOCATION_A, 0.90, List.of("same phone"));

		assertThat(second).isEqualTo(first);
		assertThat(service.pendingReviews()).singleElement().satisfies(review -> {
			assertThat(review.matchScore()).isEqualTo(0.82);
			assertThat(review.reasons()).containsExactly("similar name", "same address");
		});
	}

	@Test
	void mergesTheDuplicateIntoTheChosenCanonicalLocation() {
		UUID reviewId = service.suggest(LOCATION_A, LOCATION_B, 0.90, List.of("same address"));

		service.merge(reviewId, LOCATION_A, "admin@example.com");

		assertThat(value("SELECT status FROM restaurant_locations WHERE id = ?", LOCATION_B))
				.isEqualTo("REMOVED");
		assertThat(value("SELECT CAST(merged_into_id AS VARCHAR) FROM restaurant_locations WHERE id = ?", LOCATION_B))
				.isEqualTo(LOCATION_A.toString());
		assertThat(value("SELECT CAST(merged_into_id AS VARCHAR) FROM restaurants WHERE id = ?", RESTAURANT_B))
				.isEqualTo(RESTAURANT_A.toString());
		assertThat(service.pendingReviews()).isEmpty();
		assertThat(value("SELECT status FROM restaurant_duplicate_reviews WHERE id = ?", reviewId))
				.isEqualTo("MERGED");
	}

	@Test
	void dismissesAFalsePositiveWithoutChangingLocations() {
		UUID reviewId = service.suggest(LOCATION_A, LOCATION_B, 0.55, List.of("similar name"));

		service.dismiss(reviewId, "admin@example.com");

		assertThat(value("SELECT status FROM restaurant_duplicate_reviews WHERE id = ?", reviewId))
				.isEqualTo("DISMISSED");
		assertThat(value("SELECT status FROM restaurant_locations WHERE id = ?", LOCATION_B))
				.isEqualTo("ACTIVE");
	}

	@Test
	void doesNotAllowAReviewToBeCompletedTwice() {
		UUID reviewId = service.suggest(LOCATION_A, LOCATION_B, 0.55, List.of("similar name"));
		service.dismiss(reviewId, "admin@example.com");

		assertThatThrownBy(() -> service.merge(reviewId, LOCATION_A, "admin@example.com"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already complete");
	}

	private void insertRestaurant(UUID id, String name) {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, name, name.toLowerCase());
	}

	private void insertLocation(UUID id, UUID restaurantId, String address) {
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, ?, 'Boston', 'MA', 'US', 'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", id, restaurantId, address);
	}

	private String value(String sql, UUID id) {
		return jdbcTemplate.queryForObject(sql, String.class, id);
	}
}
