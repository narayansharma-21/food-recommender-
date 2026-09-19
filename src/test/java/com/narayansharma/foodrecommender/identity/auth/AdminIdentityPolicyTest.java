package com.narayansharma.foodrecommender.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminIdentityPolicyTest {
	@Test
	void matchesOnlyExactConfiguredProviderSubjects() {
		AdminIdentityPolicy policy = new AdminIdentityPolicy(
				"firebase:owner-id, firebase:second-id");

		assertThat(policy.isAdmin("firebase", "owner-id")).isTrue();
		assertThat(policy.isAdmin("firebase", "second-id")).isTrue();
		assertThat(policy.isAdmin("firebase", "owner")).isFalse();
		assertThat(policy.isAdmin("other", "owner-id")).isFalse();
	}
}
