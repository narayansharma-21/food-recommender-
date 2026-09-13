package com.narayansharma.foodrecommender.catalog.dish.embedding;

import java.time.Instant;
import java.util.UUID;

public record StoredDishEmbedding(
		UUID id,
		UUID dishConceptId,
		String provider,
		String modelVersion,
		int dimensions,
		String inputSha256,
		Instant createdAt,
		boolean created) {
}
