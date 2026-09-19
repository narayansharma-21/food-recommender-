package com.narayansharma.foodrecommender.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RetryFailedJobRequest(
		@NotBlank @Size(max = 1000) String reason) {
}
