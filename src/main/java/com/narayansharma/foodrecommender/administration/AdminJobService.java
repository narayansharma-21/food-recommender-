package com.narayansharma.foodrecommender.administration;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AdminJobService {
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public AdminJobService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public List<FailedJobView> failedJobs(int limit) {
		if (limit < 1 || limit > 100) {
			throw new IllegalArgumentException("Failed job limit must be between 1 and 100");
		}
		return jdbcTemplate.query("""
				SELECT id, job_type, attempts, last_error, created_at, updated_at
				FROM background_jobs
				WHERE status = 'FAILED'
				ORDER BY updated_at DESC, id
				LIMIT ?
				""", (resultSet, rowNumber) -> new FailedJobView(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("job_type"),
				resultSet.getInt("attempts"),
				resultSet.getString("last_error"),
				resultSet.getTimestamp("created_at").toInstant(),
				resultSet.getTimestamp("updated_at").toInstant()), limit);
	}

	@Transactional
	public void retry(UUID jobId, UUID actorUserId, String reason) {
		validateRetry(jobId, actorUserId, reason);
		List<FailedJob> jobs = jdbcTemplate.query("""
				SELECT job_type, status, attempts, last_error
				FROM background_jobs
				WHERE id = ?
				FOR UPDATE
				""", (resultSet, rowNumber) -> new FailedJob(
				resultSet.getString("job_type"),
				resultSet.getString("status"),
				resultSet.getInt("attempts"),
				resultSet.getString("last_error")), jobId);
		if (jobs.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "The background job was not found.");
		}
		FailedJob job = jobs.getFirst();
		if (!"FAILED".equals(job.status())) {
			throw new ApiException(HttpStatus.CONFLICT, "JOB_NOT_FAILED", "Only failed jobs can be retried.");
		}
		Instant now = clock.instant();
		jdbcTemplate.update("""
				UPDATE background_jobs
				SET status = 'PENDING', attempts = 0, available_at = ?,
				    locked_at = NULL, locked_by = NULL, last_error = NULL,
				    updated_at = ?, version = version + 1
				WHERE id = ?
				""", Timestamp.from(now), Timestamp.from(now), jobId);
		jdbcTemplate.update("""
				INSERT INTO admin_audit_events (
				    id, actor_user_id, action_type, target_type, target_id,
				    reason, details_json, occurred_at
				) VALUES (?, ?, 'RETRY_BACKGROUND_JOB', 'BACKGROUND_JOB', ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				actorUserId,
				jobId,
				reason.strip(),
				details(job),
				Timestamp.from(now));
	}

	private String details(FailedJob job) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("jobType", job.jobType());
		details.put("previousAttempts", job.attempts());
		details.put("previousError", job.lastError());
		try {
			return objectMapper.writeValueAsString(details);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Admin audit details could not be serialized", exception);
		}
	}

	private void validateRetry(UUID jobId, UUID actorUserId, String reason) {
		if (jobId == null || actorUserId == null) {
			throw new IllegalArgumentException("Job and admin user IDs are required");
		}
		if (reason == null || reason.isBlank() || reason.strip().length() > 1000) {
			throw new IllegalArgumentException("Retry reason is invalid");
		}
	}

	private record FailedJob(String jobType, String status, int attempts, String lastError) {
	}
}
