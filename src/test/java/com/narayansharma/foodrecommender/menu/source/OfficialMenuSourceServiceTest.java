package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OfficialMenuSourceServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("70000000-0000-0000-0000-000000000002");
	private static final UUID MENU_ID = UUID.fromString("70000000-0000-0000-0000-000000000003");

	@Autowired
	private OfficialMenuSourceService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertMenu() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Sample Kitchen', 'sample kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '10 Main Street', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, 'main', 'Main Menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", MENU_ID, LOCATION_ID);
	}

	@Test
	void registersHtmlAndPdfSourcesIdempotently() {
		UUID firstHtml = service.register(
				MENU_ID, MenuSourceType.OFFICIAL_HTML, URI.create("HTTPS://Menu.Example.com/dinner"));
		UUID secondHtml = service.register(
				MENU_ID, MenuSourceType.OFFICIAL_HTML, URI.create("https://menu.example.com/dinner"));
		UUID pdf = service.register(
				MENU_ID, MenuSourceType.OFFICIAL_PDF, URI.create("https://menu.example.com/dinner.pdf"));

		assertThat(secondHtml).isEqualTo(firstHtml);
		assertThat(pdf).isNotEqualTo(firstHtml);
		assertThat(url(firstHtml)).isEqualTo("https://menu.example.com/dinner");
	}

	@Test
	void preservesEncodedUrlPaths() {
		UUID sourceId = service.register(
				MENU_ID, MenuSourceType.OFFICIAL_HTML, URI.create("https://example.com/menus%2Fdinner?day=friday"));

		assertThat(url(sourceId)).isEqualTo("https://example.com/menus%2Fdinner?day=friday");
	}

	@Test
	void rejectsLocalOrCredentialBearingUrls() {
		assertThatThrownBy(() -> service.register(
				MENU_ID, MenuSourceType.OFFICIAL_HTML, URI.create("http://localhost/menu")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("public HTTP");
		assertThatThrownBy(() -> service.register(
				MENU_ID, MenuSourceType.OFFICIAL_PDF, URI.create("https://user:password@example.com/menu.pdf")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("public HTTP");
	}

	@Test
	void doesNotAcceptAnUploadAsAnOfficialUrl() {
		assertThatThrownBy(() -> service.register(
				MENU_ID, MenuSourceType.USER_IMAGE, URI.create("https://example.com/menu.jpg")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("HTML or PDF");
	}

	private String url(UUID sourceId) {
		return jdbcTemplate.queryForObject(
				"SELECT origin_url FROM menu_sources WHERE id = ?",
				String.class,
				sourceId);
	}
}
