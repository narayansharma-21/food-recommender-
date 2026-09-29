package com.narayansharma.foodrecommender.catalog.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantQueryServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("12000000-0000-0000-0000-000000000001");

	@Autowired
	private RestaurantQueryService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertRestaurant() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (
				    id, display_name, normalized_name, website_url, created_at, updated_at
				) VALUES (?, 'Cafe Example', 'cafe example', 'https://example.com',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, postal_code,
				    country_code, latitude, longitude, phone_e164, timezone,
				    created_at, updated_at
				) VALUES (?, ?, '1 Main St', 'Boston', 'MA', '02108', 'US',
				          42.360100, -71.058900, '+16175550100', 'America/New_York',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
	}

	@Test
	void returnsCanonicalRestaurantDetailsAndActiveLocations() {
		RestaurantView restaurant = service.get(RESTAURANT_ID);

		assertThat(restaurant.id()).isEqualTo(RESTAURANT_ID);
		assertThat(restaurant.displayName()).isEqualTo("Cafe Example");
		assertThat(restaurant.website()).hasToString("https://example.com");
		assertThat(restaurant.locations()).singleElement().satisfies(location -> {
			assertThat(location.id()).isEqualTo(LOCATION_ID);
			assertThat(location.address().city()).isEqualTo("Boston");
			assertThat(location.coordinates()).isNotNull();
		});
	}

	@Test
	void reportsAnUnknownRestaurant() {
		UUID unknown = UUID.fromString("11000000-0000-0000-0000-000000000099");

		assertThatThrownBy(() -> service.get(unknown))
				.isInstanceOf(ApiException.class)
				.hasMessage("The restaurant was not found.");
	}
}
