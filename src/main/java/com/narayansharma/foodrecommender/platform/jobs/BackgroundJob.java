package com.narayansharma.foodrecommender.platform.jobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "background_jobs")
class BackgroundJob {
	@Id
	private UUID id;

	@Column(name = "job_type", nullable = false, length = 100)
	private String jobType;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BackgroundJobStatus status;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "available_at", nullable = false)
	private Instant availableAt;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "locked_by", length = 100)
	private String lockedBy;

	@Column(name = "last_error", columnDefinition = "TEXT")
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected BackgroundJob() {
	}

	static BackgroundJob pending(String jobType, String payload, Instant availableAt, Instant now) {
		BackgroundJob job = new BackgroundJob();
		job.id = UUID.randomUUID();
		job.jobType = jobType;
		job.payload = payload;
		job.status = BackgroundJobStatus.PENDING;
		job.attempts = 0;
		job.availableAt = availableAt;
		job.createdAt = now;
		job.updatedAt = now;
		return job;
	}

	void claim(String workerId, Instant now) {
		status = BackgroundJobStatus.RUNNING;
		attempts++;
		lockedAt = now;
		lockedBy = workerId;
		updatedAt = now;
	}

	void complete(Instant now) {
		status = BackgroundJobStatus.COMPLETED;
		lockedAt = null;
		lockedBy = null;
		lastError = null;
		updatedAt = now;
	}

	void requireOwner(String workerId) {
		if (status != BackgroundJobStatus.RUNNING || !workerId.equals(lockedBy)) {
			throw new IllegalStateException("Background job claim is no longer owned by this worker");
		}
	}

	void fail(String error, int maxAttempts, Instant retryAt, Instant now) {
		status = attempts >= maxAttempts ? BackgroundJobStatus.FAILED : BackgroundJobStatus.PENDING;
		availableAt = retryAt;
		lockedAt = null;
		lockedBy = null;
		lastError = error;
		updatedAt = now;
	}

	UUID id() {
		return id;
	}

	String jobType() {
		return jobType;
	}

	String payload() {
		return payload;
	}

	BackgroundJobStatus status() {
		return status;
	}

	int attempts() {
		return attempts;
	}
}
