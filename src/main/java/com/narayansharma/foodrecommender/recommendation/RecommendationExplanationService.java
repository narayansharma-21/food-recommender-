package com.narayansharma.foodrecommender.recommendation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecommendationExplanationService {
	public List<RecommendationExplanation> explain(
			RecommendationMode mode,
			RecommendationSignals signals) {
		List<RecommendationExplanation> explanations = new ArrayList<>();
		if (signals.personalPreference().compareTo(new BigDecimal("0.050000")) > 0) {
			explanations.add(new RecommendationExplanation(
					"TASTE_MATCH", "Matches preferences supported by your ratings."));
		}
		if (signals.popularityRatingCount() > 0
				&& signals.popularity().compareTo(new BigDecimal("0.600000")) >= 0) {
			explanations.add(new RecommendationExplanation(
					"POPULAR_CHOICE", "Backed by the restaurant's available rating history."));
		}
		if (mode == RecommendationMode.TRY_SOMETHING_NEW && !signals.previouslyRated()) {
			explanations.add(new RecommendationExplanation(
					"NEW_FOR_YOU", "You have not rated this dish before."));
		}
		if (explanations.isEmpty()) {
			explanations.add(new RecommendationExplanation(
					"CURRENT_MENU", "Available on the restaurant's current menu."));
		}
		return List.copyOf(explanations);
	}
}
