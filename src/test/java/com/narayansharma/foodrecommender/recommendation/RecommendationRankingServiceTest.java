package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationRankingServiceTest {
	private final RecommendationSignalService signalService = mock(RecommendationSignalService.class);
	private final RecommendationRankingService service = new RecommendationRankingService(
			signalService, new WeightedRecommendationScorer(), new RecommendationExplanationService());

	@Test
	void ranksByScoreAndPreservesMenuOrderForTies() {
		UUID userId = UUID.randomUUID();
		RecommendationCandidate first = candidate("First");
		RecommendationCandidate second = candidate("Second");
		RecommendationCandidate third = candidate("Third");
		when(signalService.signals(userId, first)).thenReturn(signals("0.2"));
		when(signalService.signals(userId, second)).thenReturn(signals("0.8"));
		when(signalService.signals(userId, third)).thenReturn(signals("0.8"));

		assertThat(service.rank(
				userId, RecommendationMode.SAFE_BET, List.of(first, second, third), 2))
				.extracting(result -> result.candidate().displayName())
				.containsExactly("Second", "Third");
	}

	private RecommendationCandidate candidate(String name) {
		return new RecommendationCandidate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), name);
	}

	private RecommendationSignals signals(String preference) {
		return new RecommendationSignals(new BigDecimal(preference), new BigDecimal("0.5"), 1, 0, false);
	}
}
