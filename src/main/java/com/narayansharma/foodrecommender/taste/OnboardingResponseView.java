package com.narayansharma.foodrecommender.taste;

import java.time.Instant;
import java.util.UUID;

public record OnboardingResponseView(
		UUID id,
		UUID onboardingDishId,
		int preferenceScore,
		Instant updatedAt) {
}
