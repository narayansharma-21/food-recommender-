package com.narayansharma.foodrecommender.recommendation;

import java.util.List;
import java.util.UUID;

public record RecommendationCandidateSet(
		UUID menuVersionId,
		List<RecommendationCandidate> candidates) {
}
