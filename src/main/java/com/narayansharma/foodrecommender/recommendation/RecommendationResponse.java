package com.narayansharma.foodrecommender.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationResponse(
		String algorithmVersion,
		String featureVersion,
		UUID menuVersionId,
		RecommendationMode mode,
		Instant generatedAt,
		List<RecommendationItemView> items) {
}
