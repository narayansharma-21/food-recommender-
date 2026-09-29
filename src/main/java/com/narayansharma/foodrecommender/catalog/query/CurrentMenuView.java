package com.narayansharma.foodrecommender.catalog.query;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentMenuView(
		UUID menuId,
		UUID menuVersionId,
		UUID locationId,
		String displayName,
		int versionNumber,
		Instant capturedAt,
		String processingStatus,
		String sourceType,
		URI sourceUrl,
		List<MenuSectionView> sections) {
	public CurrentMenuView {
		sections = sections == null ? List.of() : List.copyOf(sections);
	}
}
