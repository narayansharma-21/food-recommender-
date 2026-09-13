package com.narayansharma.foodrecommender.menu.extraction.structured;

import java.math.BigDecimal;
import java.util.List;

public record ExtractedMenuItem(
		String name,
		String description,
		BigDecimal price,
		String currency,
		List<ExtractedModifier> modifiers) {
	public ExtractedMenuItem {
		if (name == null || modifiers == null) {
			throw new IllegalArgumentException("Extracted menu item fields are required");
		}
		modifiers = List.copyOf(modifiers);
	}
}
