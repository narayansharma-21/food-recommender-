package com.narayansharma.foodrecommender.identity.auth.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.narayansharma.foodrecommender.identity.auth.InvalidIdentityTokenException;
import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import org.junit.jupiter.api.Test;

class FirebaseIdentityTokenVerifierTest {
	private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
	private final FirebaseIdentityTokenVerifier verifier =
			new FirebaseIdentityTokenVerifier(firebaseAuth);

	@Test
	void returnsTheVerifiedFirebaseSubject() throws Exception {
		FirebaseToken firebaseToken = mock(FirebaseToken.class);
		when(firebaseToken.getUid()).thenReturn("firebase-user-123");
		when(firebaseAuth.verifyIdToken("signed-token")).thenReturn(firebaseToken);

		VerifiedIdentity identity = verifier.verify("signed-token");

		assertThat(identity).isEqualTo(new VerifiedIdentity("firebase", "firebase-user-123"));
	}

	@Test
	void rejectsBlankTokensWithoutCallingFirebase() {
		assertThatThrownBy(() -> verifier.verify(" "))
				.isInstanceOf(InvalidIdentityTokenException.class);
		verifyNoInteractions(firebaseAuth);
	}

	@Test
	void hidesFirebaseValidationDetails() throws Exception {
		when(firebaseAuth.verifyIdToken("bad-token"))
				.thenThrow(mock(FirebaseAuthException.class));

		assertThatThrownBy(() -> verifier.verify("bad-token"))
				.isInstanceOf(InvalidIdentityTokenException.class)
				.hasMessage("Firebase ID token is invalid");
	}
}
