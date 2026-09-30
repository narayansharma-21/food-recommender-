package com.narayansharma.foodrecommender.menu.status;

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
class MenuProcessingStatusServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("37000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("38000000-0000-0000-0000-000000000001");
	private static final UUID MENU_ID = UUID.fromString("39000000-0000-0000-0000-000000000001");
	private static final UUID SOURCE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID VERSION_ID = UUID.fromString("41000000-0000-0000-0000-000000000001");
	private static final UUID JOB_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
	private static final UUID FETCH_JOB_ID = UUID.fromString("43000000-0000-0000-0000-000000000001");

	@Autowired
	private MenuProcessingStatusService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertQueuedMenu() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Status Cafe', 'status cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
				) VALUES (?, ?, 'OFFICIAL_PDF', 'https://example.com/menu.pdf',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", SOURCE_ID, MENU_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at
				) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", VERSION_ID, MENU_ID, SOURCE_ID);
		jdbcTemplate.update("""
				INSERT INTO background_jobs (
				    id, job_type, payload, status, attempts, available_at, created_at, updated_at
				) VALUES (?, 'MENU_EXTRACTION', '{}', 'PENDING', 0, CURRENT_TIMESTAMP,
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", JOB_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_processing_jobs (job_id, menu_version_id, processing_stage, created_at)
				VALUES (?, ?, 'EXTRACTION', CURRENT_TIMESTAMP)
				""", JOB_ID, VERSION_ID);
	}

	@Test
	void reportsQueuedRunningAndFailedJobsWithoutLeakingErrors() {
		assertThat(service.get(MENU_ID).status()).isEqualTo("QUEUED");

		jdbcTemplate.update("""
				UPDATE background_jobs
				SET status = 'RUNNING', attempts = 1, locked_at = CURRENT_TIMESTAMP,
				    locked_by = 'worker', updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", JOB_ID);
		assertThat(service.get(MENU_ID).status()).isEqualTo("PROCESSING");

		jdbcTemplate.update("""
				UPDATE background_jobs
				SET status = 'FAILED', locked_at = NULL, locked_by = NULL,
				    last_error = 'sensitive provider failure', updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", JOB_ID);
		MenuProcessingStatusView failed = service.get(MENU_ID);
		assertThat(failed.status()).isEqualTo("FAILED");
		assertThat(failed.toString()).doesNotContain("sensitive provider failure");
	}

	@Test
	void reportsReadyWhenAnExtractionExists() {
		jdbcTemplate.update("""
				INSERT INTO menu_extractions (
				    id, menu_version_id, revision_number, extraction_kind,
				    ocr_provider, ocr_provider_version, parser_version,
				    ocr_result_json, structured_result_json, field_evidence_json, created_at
				) VALUES (RANDOM_UUID(), ?, 1, 'ORIGINAL', 'test', '1', '1', '{}', '{}', '{}',
				          CURRENT_TIMESTAMP)
				""", VERSION_ID);

		assertThat(service.get(MENU_ID).status()).isEqualTo("READY");
	}

	@Test
	void reportsAwaitingSourceBeforeTheFirstVersion() {
		jdbcTemplate.update("DELETE FROM menu_processing_jobs WHERE menu_version_id = ?", VERSION_ID);
		jdbcTemplate.update("DELETE FROM background_jobs WHERE id = ?", JOB_ID);
		jdbcTemplate.update("DELETE FROM menu_versions WHERE id = ?", VERSION_ID);

		MenuProcessingStatusView status = service.get(MENU_ID);

		assertThat(status.status()).isEqualTo("AWAITING_SOURCE");
		assertThat(status.menuVersionId()).isNull();
	}

	@Test
	void reportsAnOfficialSourceFetchBeforeTheFirstVersion() {
		jdbcTemplate.update("DELETE FROM menu_processing_jobs WHERE menu_version_id = ?", VERSION_ID);
		jdbcTemplate.update("DELETE FROM background_jobs WHERE id = ?", JOB_ID);
		jdbcTemplate.update("DELETE FROM menu_versions WHERE id = ?", VERSION_ID);
		jdbcTemplate.update("""
				INSERT INTO background_jobs (
				    id, job_type, payload, status, attempts, available_at, created_at, updated_at
				) VALUES (?, 'MENU_SOURCE_FETCH', '{}', 'PENDING', 0, CURRENT_TIMESTAMP,
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", FETCH_JOB_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_source_fetch_jobs (job_id, source_id, created_at)
				VALUES (?, ?, CURRENT_TIMESTAMP)
				""", FETCH_JOB_ID, SOURCE_ID);

		assertThat(service.get(MENU_ID).status()).isEqualTo("QUEUED");

		jdbcTemplate.update("""
				UPDATE background_jobs
				SET status = 'RUNNING', attempts = 1, locked_at = CURRENT_TIMESTAMP,
				    locked_by = 'worker', updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", FETCH_JOB_ID);
		assertThat(service.get(MENU_ID).status()).isEqualTo("PROCESSING");
	}

	@Test
	void reportsAnUnknownMenu() {
		UUID unknown = UUID.fromString("39000000-0000-0000-0000-000000000099");

		assertThatThrownBy(() -> service.get(unknown))
				.isInstanceOf(ApiException.class)
				.hasMessage("The menu was not found.");
	}
}
