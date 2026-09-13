package com.narayansharma.foodrecommender.identity.auth;

public record VerifiedIdentity(String provider, String subject) {
	public VerifiedIdentity {
		if (provider == null
				|| !provider.matches("[a-z][a-z0-9_-]{0,49}")
				|| subject == null
				|| subject.isBlank()
				|| subject.length() > 200) {
			throw new IllegalArgumentException("Verified identity is invalid");
		}
	}
}
