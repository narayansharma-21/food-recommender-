package com.narayansharma.foodrecommender.taste;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TasteFeatureView(
		UUID id,
		String type,
		String key,
		String displayName,
		BigDecimal preferenceScore,
		int evidenceCount,
		List<TasteEvidenceView> evidence) {
}
