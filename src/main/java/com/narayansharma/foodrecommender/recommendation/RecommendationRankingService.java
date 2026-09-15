package com.narayansharma.foodrecommender.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecommendationRankingService {
	private final RecommendationSignalService signalService;
	private final WeightedRecommendationScorer scorer;
	private final RecommendationExplanationService explanationService;

	public RecommendationRankingService(
			RecommendationSignalService signalService,
			WeightedRecommendationScorer scorer,
			RecommendationExplanationService explanationService) {
		this.signalService = signalService;
		this.scorer = scorer;
		this.explanationService = explanationService;
	}

	public List<RankedRecommendation> rank(
			UUID userId,
			RecommendationMode mode,
			List<RecommendationCandidate> candidates,
			int limit) {
		if (userId == null || mode == null || candidates == null || limit < 1 || limit > 25) {
			throw new IllegalArgumentException("Recommendation ranking input is invalid");
		}
		List<RankedRecommendation> ranked = new ArrayList<>();
		for (RecommendationCandidate candidate : candidates) {
			RecommendationSignals signals = signalService.signals(userId, candidate);
			ranked.add(new RankedRecommendation(
					candidate,
					signals,
					scorer.score(mode, signals),
					explanationService.explain(mode, signals)));
		}
		ranked.sort(Comparator.comparing(
				(RankedRecommendation result) -> result.score().score()).reversed());
		return List.copyOf(ranked.subList(0, Math.min(limit, ranked.size())));
	}
}
