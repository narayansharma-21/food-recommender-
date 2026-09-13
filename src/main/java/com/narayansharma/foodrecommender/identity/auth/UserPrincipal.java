package com.narayansharma.foodrecommender.identity.auth;

import java.security.Principal;
import java.util.UUID;

public record UserPrincipal(
		UUID userId,
		String provider,
		String providerSubject) implements Principal {
	public UserPrincipal {
		if (userId == null || provider == null || providerSubject == null) {
			throw new IllegalArgumentException("User principal is invalid");
		}
	}

	@Override
	public String getName() {
		return userId.toString();
	}
}
