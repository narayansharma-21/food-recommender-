package com.narayansharma.foodrecommender.recommendation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecommendationItemView(
		int rank,
		UUID menuItemId,
		String displayName,
		BigDecimal score,
		String confidence,
		List<String> reasons) {
}
