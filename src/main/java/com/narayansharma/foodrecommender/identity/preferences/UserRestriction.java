package com.narayansharma.foodrecommender.identity.preferences;

import java.util.UUID;

public record UserRestriction(
		UUID id,
		RestrictionType type,
		String key,
		String displayName,
		boolean active) {
}
