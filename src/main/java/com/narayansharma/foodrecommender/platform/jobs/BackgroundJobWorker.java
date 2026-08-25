package com.narayansharma.foodrecommender.platform.jobs;

import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "platform.jobs.enabled", havingValue = "true", matchIfMissing = true)
class BackgroundJobWorker {
	private static final Logger log = LoggerFactory.getLogger(BackgroundJobWorker.class);
	private final BackgroundJobService jobService;
	private final BackgroundJobDispatcher dispatcher;
	private final String workerId = UUID.randomUUID().toString();
	private final int maxAttempts;
	private final Duration retryDelay;

	BackgroundJobWorker(
			BackgroundJobService jobService,
			BackgroundJobDispatcher dispatcher,
			@Value("${platform.jobs.max-attempts:3}") int maxAttempts,
			@Value("${platform.jobs.retry-delay:PT30S}") Duration retryDelay) {
		this.jobService = jobService;
		this.dispatcher = dispatcher;
		this.maxAttempts = maxAttempts;
		this.retryDelay = retryDelay;
	}

	@Scheduled(fixedDelayString = "${platform.jobs.poll-interval:PT1S}")
	void poll() {
		jobService.claimNext(workerId).ifPresent(this::execute);
	}

	private void execute(ClaimedJob job) {
		try {
			dispatcher.dispatch(job);
			jobService.complete(job.id(), workerId);
		} catch (Exception exception) {
			log.error("Background job failed", exception);
			try {
				jobService.fail(job.id(), workerId, exception, maxAttempts, retryDelay);
			} catch (IllegalStateException lostClaim) {
				log.warn("Background job claim was lost before failure could be recorded", lostClaim);
			}
		}
	}
}
