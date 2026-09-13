package com.narayansharma.foodrecommender.menu.extraction.structured;

import java.math.BigDecimal;

public record ExtractedModifier(String name, BigDecimal price) {
	public ExtractedModifier {
		if (name == null) {
			throw new IllegalArgumentException("Extracted modifier name is required");
		}
	}
}
