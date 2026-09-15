package com.narayansharma.foodrecommender.recommendation;

import java.math.BigDecimal;

public record RecommendationSignals(
		BigDecimal personalPreference,
		BigDecimal popularity,
		int evidenceCount,
		int popularityRatingCount,
		boolean previouslyRated) {
}
