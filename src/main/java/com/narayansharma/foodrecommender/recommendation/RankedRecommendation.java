package com.narayansharma.foodrecommender.recommendation;

import java.util.List;

public record RankedRecommendation(
		RecommendationCandidate candidate,
		RecommendationSignals signals,
		ScoredRecommendation score,
		List<RecommendationExplanation> explanations) {
}
