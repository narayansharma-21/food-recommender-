package com.narayansharma.foodrecommender.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(
		@NotNull RecommendationMode mode,
		@Min(1) @Max(25) int limit) {
}
