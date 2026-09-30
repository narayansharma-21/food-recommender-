package com.narayansharma.foodrecommender.menu.status;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuProcessingStatusService {
	private final JdbcTemplate jdbcTemplate;

	public MenuProcessingStatusService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public MenuProcessingStatusView get(UUID menuId) {
		if (menuId == null) {
			throw new IllegalArgumentException("Menu ID is required");
		}
		Instant menuUpdatedAt = requireMenu(menuId);
		MenuVersion version = latestVersion(menuId);
		if (version == null) {
			ProcessingJob sourceJob = latestSourceFetchJob(menuId);
			if (sourceJob != null) {
				return new MenuProcessingStatusView(
						menuId, null, null, null, jobStatus(sourceJob), sourceJob.updatedAt());
			}
			return new MenuProcessingStatusView(
					menuId, null, null, null, "AWAITING_SOURCE", menuUpdatedAt);
		}
		if (hasExtraction(version.id())) {
			return view(menuId, version, "READY", extractionUpdatedAt(version.id()));
		}
		ProcessingJob job = latestJob(version.id());
		if (job == null) {
			return view(menuId, version, "PROCESSING", version.capturedAt());
		}
		return view(menuId, version, jobStatus(job), job.updatedAt());
	}

	private String jobStatus(ProcessingJob job) {
		return switch (job.status()) {
			case "PENDING" -> "QUEUED";
			case "RUNNING" -> "PROCESSING";
			case "FAILED", "COMPLETED" -> "FAILED";
			default -> throw new IllegalStateException("Unknown menu processing job status");
		};
	}

	private Instant requireMenu(UUID menuId) {
		return jdbcTemplate.query("""
				SELECT updated_at FROM menus WHERE id = ?
				""", (resultSet, rowNumber) -> resultSet.getTimestamp("updated_at").toInstant(), menuId).stream()
				.findFirst()
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"MENU_NOT_FOUND",
						"The menu was not found."));
	}

	private MenuVersion latestVersion(UUID menuId) {
		return jdbcTemplate.query("""
				SELECT id, version_number, captured_at
				FROM menu_versions
				WHERE menu_id = ?
				ORDER BY captured_at DESC, version_number DESC, id
				LIMIT 1
				""", (resultSet, rowNumber) -> new MenuVersion(
				resultSet.getObject("id", UUID.class),
				resultSet.getInt("version_number"),
				resultSet.getTimestamp("captured_at").toInstant()), menuId).stream()
				.findFirst()
				.orElse(null);
	}

	private boolean hasExtraction(UUID versionId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_extractions WHERE menu_version_id = ?",
				Integer.class,
				versionId);
		return count != null && count > 0;
	}

	private Instant extractionUpdatedAt(UUID versionId) {
		Timestamp createdAt = jdbcTemplate.queryForObject(
				"SELECT MAX(created_at) FROM menu_extractions WHERE menu_version_id = ?",
				Timestamp.class,
				versionId);
		return createdAt == null ? null : createdAt.toInstant();
	}

	private ProcessingJob latestJob(UUID versionId) {
		return jdbcTemplate.query("""
				SELECT job.status, job.updated_at
				FROM menu_processing_jobs link
				JOIN background_jobs job ON job.id = link.job_id
				WHERE link.menu_version_id = ? AND link.processing_stage = 'EXTRACTION'
				ORDER BY link.created_at DESC, link.job_id
				LIMIT 1
				""", (resultSet, rowNumber) -> new ProcessingJob(
				resultSet.getString("status"),
				resultSet.getTimestamp("updated_at").toInstant()), versionId).stream()
				.findFirst()
				.orElse(null);
	}

	private ProcessingJob latestSourceFetchJob(UUID menuId) {
		return jdbcTemplate.query("""
				SELECT job.status, job.updated_at
				FROM menu_source_fetch_jobs link
				JOIN menu_sources source ON source.id = link.source_id
				JOIN background_jobs job ON job.id = link.job_id
				WHERE source.menu_id = ?
				ORDER BY link.created_at DESC, link.job_id
				LIMIT 1
				""", (resultSet, rowNumber) -> new ProcessingJob(
				resultSet.getString("status"),
				resultSet.getTimestamp("updated_at").toInstant()), menuId).stream()
				.findFirst()
				.orElse(null);
	}

	private MenuProcessingStatusView view(
			UUID menuId,
			MenuVersion version,
			String status,
			Instant updatedAt) {
		return new MenuProcessingStatusView(
				menuId,
				version.id(),
				version.versionNumber(),
				version.capturedAt(),
				status,
				updatedAt);
	}

	private record MenuVersion(UUID id, int versionNumber, Instant capturedAt) {
	}

	private record ProcessingJob(String status, Instant updatedAt) {
	}
}
