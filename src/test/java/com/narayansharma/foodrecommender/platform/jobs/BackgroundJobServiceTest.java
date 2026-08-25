package com.narayansharma.foodrecommender.platform.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BackgroundJobServiceTest {
	@Autowired
	private BackgroundJobService jobService;

	@Autowired
	private BackgroundJobRepository repository;

	@Test
	void enqueuesClaimsAndCompletesJob() {
		UUID jobId = jobService.enqueue("MENU_EXTRACT", "{\"menuId\":\"123\"}");

		ClaimedJob claimedJob = jobService.claimNext("test-worker").orElseThrow();
		assertThat(claimedJob.id()).isEqualTo(jobId);
		assertThat(claimedJob.attempt()).isEqualTo(1);

		jobService.complete(jobId, "test-worker");

		assertThat(repository.findById(jobId).orElseThrow().status())
				.isEqualTo(BackgroundJobStatus.COMPLETED);
	}

	@Test
	void movesJobToFailedAfterMaximumAttempts() {
		UUID jobId = jobService.enqueue("MENU_EXTRACT", "{}");
		jobService.claimNext("test-worker").orElseThrow();

		jobService.fail(jobId, "test-worker", new IllegalStateException("OCR unavailable"), 1, Duration.ZERO);

		BackgroundJob failedJob = repository.findById(jobId).orElseThrow();
		assertThat(failedJob.status()).isEqualTo(BackgroundJobStatus.FAILED);
		assertThat(jobService.claimNext("test-worker")).isEmpty();
	}

	@Test
	void rejectsInvalidJobType() {
		assertThatThrownBy(() -> jobService.enqueue("menu extract", "{}"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void preventsAnotherWorkerFromCompletingClaimedJob() {
		UUID jobId = jobService.enqueue("MENU_EXTRACT", "{}");
		jobService.claimNext("owning-worker").orElseThrow();

		assertThatThrownBy(() -> jobService.complete(jobId, "different-worker"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("no longer owned");
	}
}
