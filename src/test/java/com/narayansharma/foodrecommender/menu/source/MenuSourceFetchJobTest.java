package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Import(MenuSourceFetchJobTest.FetchConfiguration.class)
@Transactional
class MenuSourceFetchJobTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("82000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("82000000-0000-0000-0000-000000000002");
	private static final UUID MENU_ID = UUID.fromString("82000000-0000-0000-0000-000000000003");
	private static final UUID SOURCE_ID = UUID.fromString("82000000-0000-0000-0000-000000000004");

	@Autowired
	private MenuSourceFetchJobQueue queue;

	@Autowired
	private OfficialMenuFetchService fetchService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void insertSource() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Job Kitchen', 'job kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '20 Main Street', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, 'main', 'Main Menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", MENU_ID, LOCATION_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, origin_url, created_at, updated_at
				) VALUES (?, ?, 'OFFICIAL_HTML', 'https://menu.example.com',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", SOURCE_ID, MENU_ID);
	}

	@Test
	void queuesAndHandlesAnOfficialMenuFetch() throws Exception {
		UUID jobId = queue.enqueue(SOURCE_ID);
		UUID repeatedJobId = queue.enqueue(SOURCE_ID);
		entityManager.flush();

		String payload = jdbcTemplate.queryForObject(
				"SELECT payload FROM background_jobs WHERE id = ?",
				String.class,
				jobId);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT source_id FROM menu_source_fetch_jobs WHERE job_id = ?",
				UUID.class,
				jobId)).isEqualTo(SOURCE_ID);
		assertThat(repeatedJobId).isEqualTo(jobId);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_source_fetch_jobs WHERE source_id = ?",
				Integer.class,
				SOURCE_ID)).isOne();

		new MenuSourceFetchJobHandler(objectMapper, fetchService).handle(payload);

		verify(fetchService).fetch(SOURCE_ID);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FetchConfiguration {
		@Bean
		@Primary
		OfficialMenuFetchService testOfficialMenuFetchService() {
			return mock(OfficialMenuFetchService.class);
		}
	}
}
