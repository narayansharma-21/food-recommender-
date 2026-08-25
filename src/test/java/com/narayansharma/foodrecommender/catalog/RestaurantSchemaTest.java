package com.narayansharma.foodrecommender.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesRestaurantAndLocationWithoutRequiringAMenu() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();

		jdbcTemplate.update("""
				INSERT INTO restaurants (
				    id, display_name, normalized_name, website_url, created_at, updated_at
				) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId, "Sample Kitchen", "sample kitchen", "https://example.com");
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, postal_code,
				    country_code, latitude, longitude, timezone, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
				locationId,
				restaurantId,
				"123 Main Street",
				"New York",
				"NY",
				"10001",
				"US",
				40.750000,
				-73.990000,
				"America/New_York");

		Integer locationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM restaurant_locations WHERE restaurant_id = ?",
				Integer.class,
				restaurantId);

		assertThat(locationCount).isEqualTo(1);
	}
}
