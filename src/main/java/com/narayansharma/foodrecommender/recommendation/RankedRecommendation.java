package com.narayansharma.foodrecommender.recommendation;

import java.util.List;

public record RankedRecommendation(
		RecommendationCandidate candidate,
		ScoredRecommendation score,
		List<RecommendationExplanation> explanations) {
}
