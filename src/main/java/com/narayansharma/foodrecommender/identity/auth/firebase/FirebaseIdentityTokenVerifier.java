package com.narayansharma.foodrecommender.identity.auth.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.narayansharma.foodrecommender.identity.auth.IdentityTokenVerifier;
import com.narayansharma.foodrecommender.identity.auth.InvalidIdentityTokenException;
import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "identity.firebase", name = "enabled", havingValue = "true")
public class FirebaseIdentityTokenVerifier implements IdentityTokenVerifier {
	private static final int MAX_TOKEN_LENGTH = 16_384;

	private final FirebaseAuth firebaseAuth;

	public FirebaseIdentityTokenVerifier(FirebaseAuth firebaseAuth) {
		this.firebaseAuth = firebaseAuth;
	}

	@Override
	public VerifiedIdentity verify(String idToken) {
		if (idToken == null || idToken.isBlank() || idToken.length() > MAX_TOKEN_LENGTH) {
			throw new InvalidIdentityTokenException("Firebase ID token is invalid");
		}
		try {
			FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
			return new VerifiedIdentity("firebase", token.getUid());
		} catch (FirebaseAuthException | IllegalArgumentException exception) {
			throw new InvalidIdentityTokenException("Firebase ID token is invalid", exception);
		}
	}
}
