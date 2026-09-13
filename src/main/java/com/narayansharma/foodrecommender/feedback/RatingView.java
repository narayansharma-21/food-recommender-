package com.narayansharma.foodrecommender.feedback;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RatingView(
		UUID id,
		UUID menuItemId,
		UUID dishConceptId,
		int score,
		Boolean wouldOrderAgain,
		String comment,
		List<String> tags,
		Instant createdAt,
		Instant updatedAt) {
}
