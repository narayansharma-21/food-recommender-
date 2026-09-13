package com.narayansharma.foodrecommender.identity.profile;

import java.util.UUID;

public record UserProfile(
		UUID userId,
		String displayName,
		String homeCity,
		String preferredLocale) {
}
