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
class CurrentMenuQueryServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("21000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("22000000-0000-0000-0000-000000000001");
	private static final UUID MENU_ID = UUID.fromString("23000000-0000-0000-0000-000000000001");
	private static final UUID SOURCE_ID = UUID.fromString("24000000-0000-0000-0000-000000000001");
	private static final UUID VERSION_ID = UUID.fromString("25000000-0000-0000-0000-000000000001");
	private static final UUID SECTION_ID = UUID.fromString("26000000-0000-0000-0000-000000000001");
	private static final UUID ITEM_ID = UUID.fromString("27000000-0000-0000-0000-000000000001");

	@Autowired
	private CurrentMenuQueryService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertMenu() {
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
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, 'main', 'Main Menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", MENU_ID, LOCATION_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, origin_url, created_at, updated_at
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://example.com/menu',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", SOURCE_ID, MENU_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", VERSION_ID, MENU_ID, SOURCE_ID);
	}

	@Test
	void returnsProcessingUntilTheMenuHasBeenExtracted() {
		CurrentMenuView menu = service.get(RESTAURANT_ID);

		assertThat(menu.menuVersionId()).isEqualTo(VERSION_ID);
		assertThat(menu.extractionStatus()).isEqualTo("PROCESSING");
		assertThat(menu.sections()).isEmpty();
	}

	@Test
	void returnsOrderedStructuredMenuContent() {
		jdbcTemplate.update("""
				INSERT INTO menu_extractions (
				    id, menu_version_id, revision_number, extraction_kind,
				    ocr_provider, ocr_provider_version, parser_version,
				    ocr_result_json, structured_result_json, field_evidence_json, created_at
				) VALUES (RANDOM_UUID(), ?, 1, 'ORIGINAL', 'test', '1', '1', '{}', '{}', '{}',
				          CURRENT_TIMESTAMP)
				""", VERSION_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_sections (id, menu_version_id, display_name, display_order)
				VALUES (?, ?, 'Entrees', 0)
				""", SECTION_ID, VERSION_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_items (
				    id, menu_section_id, display_name, description,
				    price_amount, price_currency, display_order
				) VALUES (?, ?, 'Pasta', 'Tomato and basil', 18.50, 'USD', 0)
				""", ITEM_ID, SECTION_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_item_modifiers (
				    id, menu_item_id, display_name, price_amount, display_order
				) VALUES (RANDOM_UUID(), ?, 'Add chicken', 4.00, 0)
				""", ITEM_ID);

		CurrentMenuView menu = service.get(RESTAURANT_ID);

		assertThat(menu.extractionStatus()).isEqualTo("READY");
		assertThat(menu.sourceUrl()).hasToString("https://example.com/menu");
		assertThat(menu.sections()).singleElement().satisfies(section ->
				assertThat(section.items()).singleElement().satisfies(item -> {
					assertThat(item.displayName()).isEqualTo("Pasta");
					assertThat(item.priceAmount()).isEqualByComparingTo("18.50");
					assertThat(item.modifiers()).singleElement().satisfies(modifier ->
							assertThat(modifier.displayName()).isEqualTo("Add chicken"));
				}));
	}

	@Test
	void reportsWhenNoMenuExists() {
		jdbcTemplate.update("DELETE FROM menu_versions WHERE id = ?", VERSION_ID);

		assertThatThrownBy(() -> service.get(RESTAURANT_ID))
				.isInstanceOf(ApiException.class)
				.hasMessage("No menu was found for the restaurant.");
	}
}
