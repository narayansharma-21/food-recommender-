package com.narayansharma.foodrecommender.menu.source;

import java.util.UUID;

public record UploadedMenuImageSource(
		UUID sourceId,
		UUID versionId,
		String objectKey,
		String mediaType,
		long sizeBytes,
		String sha256) {
}
