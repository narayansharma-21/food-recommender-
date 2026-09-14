package com.narayansharma.foodrecommender.taste;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PreferenceScorer {
	private static final BigDecimal PRIOR_WEIGHT = new BigDecimal("2");

	public BigDecimal score(List<Integer> ratings) {
		if (ratings == null || ratings.isEmpty()) {
			throw new IllegalArgumentException("At least one rating is required");
		}
		if (ratings.stream().anyMatch(rating -> rating == null || rating < 1 || rating > 5)) {
			throw new IllegalArgumentException("Ratings must be between 1 and 5");
		}
		BigDecimal total = ratings.stream()
				.map(rating -> BigDecimal.valueOf(rating - 3).divide(new BigDecimal("2")))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return total.divide(BigDecimal.valueOf(ratings.size()).add(PRIOR_WEIGHT), 6, RoundingMode.HALF_UP);
	}
}
