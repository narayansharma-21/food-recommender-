package com.narayansharma.foodrecommender.taste;

import java.math.BigDecimal;
import java.util.UUID;

public record TasteEvidenceView(
		String sourceType,
		UUID sourceId,
		BigDecimal contribution) {
}
