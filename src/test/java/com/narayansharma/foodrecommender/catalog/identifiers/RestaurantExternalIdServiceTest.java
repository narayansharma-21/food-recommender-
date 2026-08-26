package com.narayansharma.foodrecommender.catalog.identifiers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.duplicates.RestaurantDuplicateReviewService;
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
class RestaurantExternalIdServiceTest {
	private static final UUID RESTAURANT_A = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID RESTAURANT_B = UUID.fromString("30000000-0000-0000-0000-000000000002");
	private static final UUID LOCATION_A = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_B = UUID.fromString("40000000-0000-0000-0000-000000000002");
	private static final ExternalRestaurantId OVERTURE_ID = new ExternalRestaurantId("overture", "place-123");

	@Autowired
	private RestaurantExternalIdService service;

	@Autowired
	private RestaurantDuplicateReviewService duplicateReviewService;

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
	void attachesAndResolvesAProviderId() {
		service.attach(OVERTURE_ID, LOCATION_A);

		ResolvedRestaurantLocation resolved = service.resolve(OVERTURE_ID).orElseThrow();

		assertThat(resolved.restaurantId()).isEqualTo(RESTAURANT_A);
		assertThat(resolved.locationId()).isEqualTo(LOCATION_A);
		assertThat(service.findForLocation(LOCATION_A)).containsExactly(OVERTURE_ID);
	}

	@Test
	void preventsOneProviderIdFromPointingAtTwoActiveLocations() {
		service.attach(OVERTURE_ID, LOCATION_A);

		assertThatThrownBy(() -> service.attach(OVERTURE_ID, LOCATION_B))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("another location");
	}

	@Test
	void resolvesOldProviderIdsAfterALocationMerge() {
		service.attach(OVERTURE_ID, LOCATION_B);
		UUID reviewId = duplicateReviewService.suggest(
				LOCATION_A, LOCATION_B, 0.90, List.of("same address"));

		duplicateReviewService.merge(reviewId, LOCATION_A, "admin@example.com");

		ResolvedRestaurantLocation resolved = service.resolve(OVERTURE_ID).orElseThrow();
		assertThat(resolved.restaurantId()).isEqualTo(RESTAURANT_A);
		assertThat(resolved.locationId()).isEqualTo(LOCATION_A);
		assertThat(service.findForLocation(LOCATION_A)).containsExactly(OVERTURE_ID);
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
}
