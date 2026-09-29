package com.narayansharma.foodrecommender.menu.source;

import java.util.UUID;

public record MenuUploadResponse(
		UUID menuId,
		UUID sourceId,
		UUID menuVersionId,
		String extractionStatus,
		String mediaType,
		long sizeBytes,
		String sha256) {
}
