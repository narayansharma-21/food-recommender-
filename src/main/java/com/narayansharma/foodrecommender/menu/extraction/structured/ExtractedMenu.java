package com.narayansharma.foodrecommender.menu.extraction.structured;

import java.util.List;

public record ExtractedMenu(List<ExtractedMenuSection> sections) {
	public ExtractedMenu {
		if (sections == null) {
			throw new IllegalArgumentException("Extracted menu sections are required");
		}
		sections = List.copyOf(sections);
	}
}
