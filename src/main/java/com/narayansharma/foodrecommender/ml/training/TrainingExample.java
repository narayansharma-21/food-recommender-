package com.narayansharma.foodrecommender.ml.training;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrainingExample(
		String schemaVersion,
		UUID recommendationResultId,
		UUID ratingId,
		UUID userId,
		UUID restaurantId,
		UUID menuItemId,
		UUID dishConceptId,
		Instant recommendedAt,
		Instant ratedAt,
		String mode,
		String algorithmVersion,
		String featureVersion,
		int rank,
		BigDecimal personalPreference,
		BigDecimal popularity,
		int tasteEvidenceCount,
		int popularityRatingCount,
		boolean previouslyRated,
		int ratingScore,
		Boolean wouldOrderAgain) {
}
