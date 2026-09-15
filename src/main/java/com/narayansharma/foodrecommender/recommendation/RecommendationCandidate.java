package com.narayansharma.foodrecommender.recommendation;

import java.util.UUID;

public record RecommendationCandidate(
		UUID menuVersionId,
		UUID menuItemId,
		UUID dishConceptId,
		String displayName) {
}
