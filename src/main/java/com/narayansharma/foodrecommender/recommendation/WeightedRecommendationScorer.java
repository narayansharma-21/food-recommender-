package com.narayansharma.foodrecommender.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class WeightedRecommendationScorer {
	public ScoredRecommendation score(RecommendationMode mode, RecommendationSignals signals) {
		if (mode == null || signals == null) {
			throw new IllegalArgumentException("Recommendation mode and signals are required");
		}
		BigDecimal personal = requireRange(signals.personalPreference(), "Personal preference", -1, 1);
		BigDecimal popularity = requireRange(signals.popularity(), "Popularity", 0, 1);
		BigDecimal score = switch (mode) {
			case SAFE_BET -> personal.multiply(new BigDecimal("0.700000"))
					.add(popularity.multiply(new BigDecimal("0.300000")));
			case TRY_SOMETHING_NEW -> personal.multiply(new BigDecimal("0.500000"))
					.add(popularity.multiply(new BigDecimal("0.200000")))
					.add(signals.previouslyRated() ? BigDecimal.ZERO : new BigDecimal("0.300000"));
		};
		String confidence = signals.evidenceCount() >= 3
				? "HIGH"
				: signals.evidenceCount() > 0 ? "MEDIUM" : "LOW";
		return new ScoredRecommendation(score.setScale(6, RoundingMode.HALF_UP), confidence);
	}

	private BigDecimal requireRange(BigDecimal value, String name, int minimum, int maximum) {
		if (value == null
				|| value.compareTo(BigDecimal.valueOf(minimum)) < 0
				|| value.compareTo(BigDecimal.valueOf(maximum)) > 0) {
			throw new IllegalArgumentException(name + " is outside its supported range");
		}
		return value;
	}
}
