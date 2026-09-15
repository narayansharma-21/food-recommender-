package com.narayansharma.foodrecommender.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationResponse(
		String algorithmVersion,
		UUID menuVersionId,
		RecommendationMode mode,
		Instant generatedAt,
		List<RecommendationItemView> items) {
}
