package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.platform.jobs.BackgroundJobService;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
class MenuSourceFetchJobQueue {
	private final BackgroundJobService jobService;
	private final ObjectMapper objectMapper;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final EntityManager entityManager;

	MenuSourceFetchJobQueue(
			BackgroundJobService jobService,
			ObjectMapper objectMapper,
			JdbcTemplate jdbcTemplate,
			Clock clock,
			EntityManager entityManager) {
		this.jobService = jobService;
		this.objectMapper = objectMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.entityManager = entityManager;
	}

	@Transactional
	UUID enqueue(UUID sourceId) {
		if (sourceId == null) {
			throw new IllegalArgumentException("Official menu source is required");
		}
		lockSource(sourceId);
		List<UUID> activeJobs = jdbcTemplate.query("""
				SELECT job.id
				FROM menu_source_fetch_jobs link
				JOIN background_jobs job ON job.id = link.job_id
				WHERE link.source_id = ? AND job.status IN ('PENDING', 'RUNNING')
				ORDER BY link.created_at DESC, link.job_id
				LIMIT 1
				""", (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), sourceId);
		if (!activeJobs.isEmpty()) {
			return activeJobs.getFirst();
		}
		try {
			String payload = objectMapper.writeValueAsString(new MenuSourceFetchJobPayload(sourceId));
			UUID jobId = jobService.enqueue(MenuSourceFetchJobHandler.JOB_TYPE, payload);
			entityManager.flush();
			jdbcTemplate.update("""
					INSERT INTO menu_source_fetch_jobs (job_id, source_id, created_at)
					VALUES (?, ?, ?)
					""", jobId, sourceId, Timestamp.from(clock.instant()));
			return jobId;
		} catch (JacksonException exception) {
			throw new IllegalStateException("Menu source fetch job could not be serialized", exception);
		}
	}

	private void lockSource(UUID sourceId) {
		List<UUID> sources = jdbcTemplate.query(
				"SELECT id FROM menu_sources WHERE id = ? FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				sourceId);
		if (sources.isEmpty()) {
			throw new IllegalArgumentException("Unknown official menu source: " + sourceId);
		}
	}
}
