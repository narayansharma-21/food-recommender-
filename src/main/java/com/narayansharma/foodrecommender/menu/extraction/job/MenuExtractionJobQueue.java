package com.narayansharma.foodrecommender.menu.extraction.job;

import com.narayansharma.foodrecommender.platform.jobs.BackgroundJobService;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class MenuExtractionJobQueue {
	private final BackgroundJobService jobService;
	private final ObjectMapper objectMapper;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final EntityManager entityManager;

	public MenuExtractionJobQueue(
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
	public UUID enqueue(UUID menuVersionId) {
		if (menuVersionId == null) {
			throw new IllegalArgumentException("Menu version is required");
		}
		try {
			String payload = objectMapper.writeValueAsString(new MenuExtractionJobPayload(menuVersionId));
			UUID jobId = jobService.enqueue(MenuExtractionJobHandler.JOB_TYPE, payload);
			entityManager.flush();
			jdbcTemplate.update("""
					INSERT INTO menu_processing_jobs (
					    job_id, menu_version_id, processing_stage, created_at
					) VALUES (?, ?, 'EXTRACTION', ?)
					""", jobId, menuVersionId, Timestamp.from(clock.instant()));
			return jobId;
		} catch (JacksonException exception) {
			throw new IllegalStateException("Menu extraction job could not be serialized", exception);
		}
	}
}
