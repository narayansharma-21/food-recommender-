package com.narayansharma.foodrecommender.identity.preferences;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetUserRestrictionRequest(
		@NotBlank @Size(max = 200) String displayName,
		boolean active) {
}
