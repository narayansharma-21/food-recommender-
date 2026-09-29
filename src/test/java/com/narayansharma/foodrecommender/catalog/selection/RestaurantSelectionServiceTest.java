package com.narayansharma.foodrecommender.catalog.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.matching.RestaurantMatchLevel;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantSelectionServiceTest {
	private static final ExternalRestaurantId SOURCE_ID = new ExternalRestaurantId("overture", "selected-place");

	@Autowired
	private RestaurantSelectionService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsAndThenReusesACanonicalRestaurant() {
		insertSource(SOURCE_ID, "Cafe Example", "1 Main St", "Boston", "42.360100", "-71.058900");

		SelectedRestaurant created = service.select(SOURCE_ID);
		SelectedRestaurant selectedAgain = service.select(SOURCE_ID);

		assertThat(created.created()).isTrue();
		assertThat(created.matchLevel()).isEqualTo(RestaurantMatchLevel.NONE);
		assertThat(selectedAgain.created()).isFalse();
		assertThat(selectedAgain.matchLevel()).isEqualTo(RestaurantMatchLevel.EXACT);
		assertThat(selectedAgain.restaurantId()).isEqualTo(created.restaurantId());
		assertThat(selectedAgain.locationId()).isEqualTo(created.locationId());
		assertThat(count("SELECT COUNT(*) FROM restaurants WHERE id = ?", created.restaurantId())).isOne();
	}

	@Test
	void linksAStrongMatchInsteadOfCreatingADuplicate() {
		UUID restaurantId = UUID.fromString("31000000-0000-0000-0000-000000000001");
		UUID locationId = UUID.fromString("32000000-0000-0000-0000-000000000001");
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Cafe Example', 'cafe example', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, postal_code,
				    country_code, latitude, longitude, timezone, created_at, updated_at
				) VALUES (?, ?, '1 Main St', 'Boston', 'MA', '02108', 'US',
				          42.360100, -71.058900, 'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", locationId, restaurantId);
		insertSource(SOURCE_ID, "Cafe Example", "1 Main St", "Boston", "42.360100", "-71.058900");

		SelectedRestaurant selected = service.select(SOURCE_ID);

		assertThat(selected.created()).isFalse();
		assertThat(selected.matchLevel()).isEqualTo(RestaurantMatchLevel.LIKELY);
		assertThat(selected.restaurantId()).isEqualTo(restaurantId);
		assertThat(selected.locationId()).isEqualTo(locationId);
		assertThat(count("SELECT COUNT(*) FROM restaurants")).isOne();
	}

	@Test
	void rejectsASourceRecordWithoutACompleteAddress() {
		insertSource(SOURCE_ID, "Mobile Cafe", null, "Boston", null, null);

		assertThatThrownBy(() -> service.select(SOURCE_ID))
				.isInstanceOf(ApiException.class)
				.hasMessage("The restaurant needs a complete address before it can be selected.");
		assertThat(count("SELECT COUNT(*) FROM restaurants")).isZero();
	}

	@Test
	void queuesAnAmbiguousMatchForDuplicateReview() {
		UUID restaurantId = UUID.fromString("31000000-0000-0000-0000-000000000002");
		UUID locationId = UUID.fromString("32000000-0000-0000-0000-000000000002");
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Cafe Example', 'cafe example', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, postal_code,
				    country_code, latitude, longitude, timezone, created_at, updated_at
				) VALUES (?, ?, '99 Side St', 'Boston', 'MA', '99999', 'US',
				          42.361100, -71.058900, 'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", locationId, restaurantId);
		insertSource(SOURCE_ID, "Cafe Example", "1 Main St", "Boston", "42.360100", "-71.058900");

		SelectedRestaurant selected = service.select(SOURCE_ID);

		assertThat(selected.created()).isTrue();
		assertThat(selected.matchLevel()).isEqualTo(RestaurantMatchLevel.REVIEW);
		assertThat(selected.duplicateReviewRequired()).isTrue();
		assertThat(count("SELECT COUNT(*) FROM restaurant_duplicate_reviews WHERE status = 'PENDING'"))
				.isOne();
	}

	private void insertSource(
			ExternalRestaurantId externalId,
			String name,
			String address,
			String city,
			String latitude,
			String longitude) {
		jdbcTemplate.update("""
				INSERT INTO restaurant_source_records (
				    source, external_id, display_name, normalized_name, address_line_1,
				    city, region, postal_code, country_code, latitude, longitude,
				    category, confidence, imported_at
				) VALUES (?, ?, ?, ?, ?, ?, 'MA', '02108', 'US', ?, ?,
				          'cafe', 0.99, CURRENT_TIMESTAMP)
				""",
				externalId.source(),
				externalId.value(),
				name,
				name.toLowerCase(),
				address,
				city,
				latitude,
				longitude);
	}

	private int count(String sql, Object... arguments) {
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
		return count == null ? 0 : count;
	}
}
