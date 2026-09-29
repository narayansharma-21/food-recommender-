package com.narayansharma.foodrecommender.catalog.query;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuModifierView(
		UUID id,
		String displayName,
		BigDecimal priceAmount) {
}
