package com.narayansharma.foodrecommender.platform.jobs;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackgroundJobService {
	private static final Pattern VALID_JOB_TYPE = Pattern.compile("[A-Z][A-Z0-9_]{0,99}");
	private final BackgroundJobRepository repository;
	private final Clock clock;
	private final Duration lockTimeout;

	public BackgroundJobService(
			BackgroundJobRepository repository,
			Clock clock,
			@Value("${platform.jobs.lock-timeout:PT15M}") Duration lockTimeout) {
		this.repository = repository;
		this.clock = clock;
		this.lockTimeout = lockTimeout;
	}

	@Transactional
	public UUID enqueue(String jobType, String payload) {
		return enqueue(jobType, payload, Instant.now(clock));
	}

	@Transactional
	public UUID enqueue(String jobType, String payload, Instant availableAt) {
		if (jobType == null || !VALID_JOB_TYPE.matcher(jobType).matches()) {
			throw new IllegalArgumentException("Job type must be uppercase letters, numbers, or underscores");
		}
		if (payload == null) {
			throw new IllegalArgumentException("Job payload is required");
		}
		if (availableAt == null) {
			throw new IllegalArgumentException("Job availability time is required");
		}
		Instant now = Instant.now(clock);
		BackgroundJob job = repository.save(BackgroundJob.pending(jobType, payload, availableAt, now));
		return job.id();
	}

	@Transactional
	public Optional<ClaimedJob> claimNext(String workerId) {
		Instant now = Instant.now(clock);
		return repository.findReady(now, now.minus(lockTimeout), PageRequest.of(0, 1)).stream()
				.findFirst()
				.map(job -> {
					job.claim(workerId, now);
					return new ClaimedJob(job.id(), job.jobType(), job.payload(), job.attempts());
				});
	}

	@Transactional
	public void complete(UUID jobId, String workerId) {
		BackgroundJob job = repository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Unknown background job: " + jobId));
		job.requireOwner(workerId);
		job.complete(Instant.now(clock));
	}

	@Transactional
	public void fail(UUID jobId, String workerId, Throwable failure, int maxAttempts, Duration retryDelay) {
		BackgroundJob job = repository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Unknown background job: " + jobId));
		job.requireOwner(workerId);
		Instant now = Instant.now(clock);
		String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
		job.fail(message, maxAttempts, now.plus(retryDelay), now);
	}
}
