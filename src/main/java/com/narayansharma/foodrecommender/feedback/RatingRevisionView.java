package com.narayansharma.foodrecommender.feedback;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RatingRevisionView(
		UUID id,
		int revisionNumber,
		String changeType,
		int score,
		Boolean wouldOrderAgain,
		String comment,
		List<String> tags,
		Instant createdAt) {
}
