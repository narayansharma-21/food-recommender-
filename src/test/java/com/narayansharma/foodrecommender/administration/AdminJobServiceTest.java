package com.narayansharma.foodrecommender.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.platform.jobs.BackgroundJobService;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminJobServiceTest {
	@Autowired
	private AdminJobService adminJobService;

	@Autowired
	private BackgroundJobService backgroundJobService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void listsAndRetriesAFailedJobWithAnAuditEvent() {
		UUID actorId = insertUser();
		UUID jobId = backgroundJobService.enqueue("MENU_EXTRACTION", "{}");
		backgroundJobService.claimNext("test-worker").orElseThrow();
		backgroundJobService.fail(
				jobId,
				"test-worker",
				new IllegalStateException("OCR unavailable " + "x".repeat(5000)),
				1,
				Duration.ZERO);
		entityManager.flush();

		assertThat(adminJobService.failedJobs(10))
				.extracting(FailedJobView::id)
				.contains(jobId);

		adminJobService.retry(jobId, actorId, "Tesseract was restored");

		Map<String, Object> job = jdbcTemplate.queryForMap(
				"SELECT status, attempts, last_error FROM background_jobs WHERE id = ?", jobId);
		assertThat(job.get("STATUS")).isEqualTo("PENDING");
		assertThat(job.get("ATTEMPTS")).isEqualTo(0);
		assertThat(job.get("LAST_ERROR")).isNull();
		Map<String, Object> audit = jdbcTemplate.queryForMap(
				"SELECT actor_user_id, reason, details_json FROM admin_audit_events WHERE target_id = ?", jobId);
		assertThat(audit.get("ACTOR_USER_ID")).isEqualTo(actorId);
		assertThat(audit.get("REASON")).isEqualTo("Tesseract was restored");
		assertThat((String) audit.get("DETAILS_JSON"))
				.contains("OCR unavailable")
				.hasSizeLessThan(4000);
	}

	@Test
	void rejectsRetryingAJobThatHasNotFailed() {
		UUID actorId = insertUser();
		UUID jobId = backgroundJobService.enqueue("MENU_EXTRACTION", "{}");
		entityManager.flush();

		assertThatThrownBy(() -> adminJobService.retry(jobId, actorId, "No failure"))
				.isInstanceOf(ApiException.class)
				.hasMessage("Only failed jobs can be retried.");
	}

	private UUID insertUser() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		return userId;
	}
}
