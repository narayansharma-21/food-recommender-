package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DietaryRestrictionPolicyTest {
	private final DietaryRestrictionPolicy policy = new DietaryRestrictionPolicy();

	@Test
	void blocksKnownConflicts() {
		assertThat(policy.allows(Set.of("vegetarian"), Set.of("tomato", "beef"))).isFalse();
		assertThat(policy.allows(Set.of("vegan"), Set.of("cheese"))).isFalse();
		assertThat(policy.allows(Set.of("pescatarian"), Set.of("shrimp"))).isTrue();
	}

	@Test
	void failsClosedForUnsupportedDietaryRules() {
		assertThat(policy.allows(Set.of("kosher"), Set.of("tomato"))).isFalse();
		assertThat(policy.allows(Set.of(), Set.of("beef"))).isTrue();
	}
}
