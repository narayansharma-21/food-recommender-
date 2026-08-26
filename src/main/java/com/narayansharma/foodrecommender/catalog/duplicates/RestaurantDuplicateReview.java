package com.narayansharma.foodrecommender.catalog.duplicates;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestaurantDuplicateReview(
		UUID id,
		UUID locationAId,
		UUID locationBId,
		double matchScore,
		List<String> reasons,
		RestaurantDuplicateReviewStatus status,
		UUID canonicalLocationId,
		String reviewerReference,
		Instant createdAt,
		Instant reviewedAt) {
}
