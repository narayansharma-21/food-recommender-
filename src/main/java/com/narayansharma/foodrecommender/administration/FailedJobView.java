package com.narayansharma.foodrecommender.administration;

import java.time.Instant;
import java.util.UUID;

public record FailedJobView(
		UUID id,
		String jobType,
		int attempts,
		String lastError,
		Instant createdAt,
		Instant updatedAt) {
}
