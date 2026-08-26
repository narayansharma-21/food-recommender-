package com.narayansharma.foodrecommender.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MenuCatalogSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void keepsMultipleStructuredVersionsOfOneMenu() {
		UUID locationId = insertRestaurantLocation();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, 'main', 'Main Menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", menuId, locationId);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, origin_url, created_at, updated_at
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", sourceId, menuId);

		UUID firstVersion = insertVersion(menuId, sourceId, 1);
		UUID secondVersion = insertVersion(menuId, sourceId, 2);
		insertSectionAndItem(firstVersion, "Old Burger", "12.00");
		insertSectionAndItem(secondVersion, "New Burger", "14.00");

		Integer versionCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_versions WHERE menu_id = ?",
				Integer.class,
				menuId);
		Integer itemCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM menu_items item
				JOIN menu_sections section ON section.id = item.menu_section_id
				JOIN menu_versions version ON version.id = section.menu_version_id
				WHERE version.menu_id = ?
				""", Integer.class, menuId);

		assertThat(versionCount).isEqualTo(2);
		assertThat(itemCount).isEqualTo(2);
	}

	private UUID insertRestaurantLocation() {
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Sample Kitchen', 'sample kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", restaurantId);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '10 Main Street', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", locationId, restaurantId);
		return locationId;
	}

	private UUID insertVersion(UUID menuId, UUID sourceId, int versionNumber) {
		UUID versionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", versionId, menuId, sourceId, versionNumber);
		return versionId;
	}

	private void insertSectionAndItem(UUID versionId, String itemName, String price) {
		UUID sectionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO menu_sections (id, menu_version_id, display_name, display_order)
				VALUES (?, ?, 'Entrees', 0)
				""", sectionId, versionId);
		jdbcTemplate.update("""
				INSERT INTO menu_items (
				    id, menu_section_id, display_name, description,
				    price_amount, price_currency, display_order
				) VALUES (?, ?, ?, 'A sample item', ?, 'USD', 0)
				""", UUID.randomUUID(), sectionId, itemName, new BigDecimal(price));
	}
}
