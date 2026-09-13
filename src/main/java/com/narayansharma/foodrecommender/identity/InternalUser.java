package com.narayansharma.foodrecommender.identity;

import java.util.UUID;

public record InternalUser(UUID id, boolean created) {
	public InternalUser {
		if (id == null) {
			throw new IllegalArgumentException("Internal user ID is required");
		}
	}
}
