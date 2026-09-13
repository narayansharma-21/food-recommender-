package com.narayansharma.foodrecommender.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReportRatingRequest(
		@NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String reasonCode,
		@Size(max = 1000) String details) {
}
