package com.narayansharma.foodrecommender.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SaveRatingRequest(
		@NotNull UUID menuItemId,
		@Min(1) @Max(5) int score,
		Boolean wouldOrderAgain,
		@Size(max = 4000) String comment,
		@Size(max = 10) List<@Pattern(regexp = "[a-z][a-z0-9_]{0,49}") String> tags) {
}
