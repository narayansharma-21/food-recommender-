package com.narayansharma.foodrecommender.menu.status;

import java.time.Instant;
import java.util.UUID;

public record MenuProcessingStatusView(
		UUID menuId,
		UUID menuVersionId,
		Integer versionNumber,
		Instant capturedAt,
		String status,
		Instant updatedAt) {
}
