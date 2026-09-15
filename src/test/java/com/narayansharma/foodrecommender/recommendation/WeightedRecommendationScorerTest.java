package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WeightedRecommendationScorerTest {
	private final WeightedRecommendationScorer scorer = new WeightedRecommendationScorer();

	@Test
	void safeBetPrioritizesKnownPreferenceAndPopularity() {
		RecommendationSignals signals = new RecommendationSignals(
				new BigDecimal("0.600000"), new BigDecimal("0.800000"), 4, 2, true);

		assertThat(scorer.score(RecommendationMode.SAFE_BET, signals))
				.isEqualTo(new ScoredRecommendation(new BigDecimal("0.660000"), "HIGH"));
	}

	@Test
	void trySomethingNewAddsANoveltyBonus() {
		RecommendationSignals signals = new RecommendationSignals(
				new BigDecimal("0.200000"), new BigDecimal("0.500000"), 1, 0, false);

		assertThat(scorer.score(RecommendationMode.TRY_SOMETHING_NEW, signals))
				.isEqualTo(new ScoredRecommendation(new BigDecimal("0.500000"), "MEDIUM"));
	}

	@Test
	void coldStartStillProducesALowConfidenceScore() {
		RecommendationSignals signals = new RecommendationSignals(
				BigDecimal.ZERO, new BigDecimal("0.500000"), 0, 0, false);

		assertThat(scorer.score(RecommendationMode.SAFE_BET, signals))
				.isEqualTo(new ScoredRecommendation(new BigDecimal("0.150000"), "LOW"));
	}
}
