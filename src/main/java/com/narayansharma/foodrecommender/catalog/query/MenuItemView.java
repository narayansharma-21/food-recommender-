package com.narayansharma.foodrecommender.catalog.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuItemView(
		UUID id,
		UUID dishConceptId,
		String displayName,
		String description,
		BigDecimal priceAmount,
		String priceCurrency,
		List<MenuModifierView> modifiers) {
	public MenuItemView {
		modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
	}
}
