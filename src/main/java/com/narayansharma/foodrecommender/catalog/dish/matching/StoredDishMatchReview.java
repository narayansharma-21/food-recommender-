package com.narayansharma.foodrecommender.catalog.dish.matching;

import java.time.Instant;
import java.util.UUID;

public record StoredDishMatchReview(
		UUID id,
		UUID menuItemId,
		UUID suggestedDishConceptId,
		double confidence,
		String matchMethod,
		String status,
		String reviewerReference,
		String resolutionNote,
		Instant createdAt,
		Instant resolvedAt,
		boolean created) {
}
