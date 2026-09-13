package com.narayansharma.foodrecommender.menu.extraction.structured;

import java.util.List;

public record ExtractedMenuSection(String name, List<ExtractedMenuItem> items) {
	public ExtractedMenuSection {
		if (name == null || items == null) {
			throw new IllegalArgumentException("Extracted menu section fields are required");
		}
		items = List.copyOf(items);
	}
}
