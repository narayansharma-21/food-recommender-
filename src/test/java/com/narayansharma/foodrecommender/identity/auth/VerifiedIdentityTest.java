package com.narayansharma.foodrecommender.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VerifiedIdentityTest {
	@Test
	void acceptsAProviderOwnedSubject() {
		VerifiedIdentity identity = new VerifiedIdentity("firebase", "firebase-user-123");

		assertThat(identity.provider()).isEqualTo("firebase");
		assertThat(identity.subject()).isEqualTo("firebase-user-123");
	}

	@Test
	void rejectsBlankSubjects() {
		assertThatThrownBy(() -> new VerifiedIdentity("firebase", " "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
