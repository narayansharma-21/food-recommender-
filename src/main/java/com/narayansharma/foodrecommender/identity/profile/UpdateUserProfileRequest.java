package com.narayansharma.foodrecommender.identity.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
		@Size(max = 100) String displayName,
		@Size(max = 100) String homeCity,
		@NotBlank @Size(max = 20) String preferredLocale) {
}
