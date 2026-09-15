package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecommendationExplanationServiceTest {
	private final RecommendationExplanationService service = new RecommendationExplanationService();

	@Test
	void explainsTastePopularityAndNoveltyFromSignals() {
		RecommendationSignals signals = new RecommendationSignals(
				new BigDecimal("0.4"), new BigDecimal("0.7"), 3, 2, false);

		assertThat(service.explain(RecommendationMode.TRY_SOMETHING_NEW, signals))
				.extracting(RecommendationExplanation::code)
				.containsExactly("TASTE_MATCH", "POPULAR_CHOICE", "NEW_FOR_YOU");
	}

	@Test
	void alwaysProvidesAFallbackReason() {
		RecommendationSignals signals = new RecommendationSignals(
				new BigDecimal("-0.2"), new BigDecimal("0.4"), 1, 0, true);

		assertThat(service.explain(RecommendationMode.SAFE_BET, signals))
				.extracting(RecommendationExplanation::code)
				.containsExactly("CURRENT_MENU");
	}
}
