package com.narayansharma.foodrecommender.menu.version;

import java.time.Instant;
import java.util.UUID;

public record CapturedMenuVersion(
		UUID versionId,
		UUID menuId,
		UUID sourceId,
		int versionNumber,
		Instant capturedAt) {
}
