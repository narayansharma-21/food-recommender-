package com.narayansharma.foodrecommender.taste;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreferenceScorerTest {
	private final PreferenceScorer scorer = new PreferenceScorer();

	@Test
	void shrinksOneExtremeRatingTowardNeutral() {
		assertThat(scorer.score(List.of(5))).isEqualByComparingTo(new BigDecimal("0.333333"));
		assertThat(scorer.score(List.of(1))).isEqualByComparingTo(new BigDecimal("-0.333333"));
	}

	@Test
	void gainsConfidenceFromRepeatedEvidence() {
		assertThat(scorer.score(List.of(5, 5, 5, 5)))
				.isEqualByComparingTo(new BigDecimal("0.666667"));
		assertThat(scorer.score(List.of(1, 5))).isEqualByComparingTo(BigDecimal.ZERO);
	}
}
