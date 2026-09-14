package com.narayansharma.foodrecommender.taste;

import java.time.Instant;
import java.util.List;

public record TasteProfileView(
		String calculationVersion,
		Instant calculatedAt,
		List<TasteFeatureView> features) {
}
