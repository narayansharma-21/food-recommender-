package com.narayansharma.foodrecommender.menu.extraction.persistence;

import java.time.Instant;
import java.util.UUID;

public record StoredOriginalExtraction(
		UUID id,
		UUID menuVersionId,
		Instant createdAt,
		boolean created) {
}
