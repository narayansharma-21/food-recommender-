package com.narayansharma.foodrecommender.menu.catalog;

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
class MenuCatalogServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("33000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("34000000-0000-0000-0000-000000000001");

	@Autowired
	private MenuCatalogService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertRestaurant() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Cafe Example', 'cafe example', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Main St', 'Boston', 'MA', 'US', 'America/New_York',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
	}

	@Test
	void createsAndThenReusesAMenuKeyForTheLocation() {
		MenuDefinition created = service.findOrCreate(LOCATION_ID, "main", "Main Menu");
		MenuDefinition existing = service.findOrCreate(LOCATION_ID, "main", "Different Name");

		assertThat(created.created()).isTrue();
		assertThat(existing.created()).isFalse();
		assertThat(existing.menuId()).isEqualTo(created.menuId());
		assertThat(existing.displayName()).isEqualTo("Main Menu");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menus WHERE restaurant_location_id = ?",
				Integer.class,
				LOCATION_ID)).isOne();
	}

	@Test
	void rejectsAnUnknownOrInactiveLocation() {
		jdbcTemplate.update("UPDATE restaurant_locations SET status = 'CLOSED' WHERE id = ?", LOCATION_ID);

		assertThatThrownBy(() -> service.findOrCreate(LOCATION_ID, "main", "Main Menu"))
				.isInstanceOf(ApiException.class)
				.hasMessage("The active restaurant location was not found.");
	}
}
