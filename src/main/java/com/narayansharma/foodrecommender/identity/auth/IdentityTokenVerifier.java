package com.narayansharma.foodrecommender.identity.auth;

public interface IdentityTokenVerifier {
	VerifiedIdentity verify(String idToken);
}
