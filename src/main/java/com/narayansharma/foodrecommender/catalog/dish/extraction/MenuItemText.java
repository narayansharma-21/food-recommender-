package com.narayansharma.foodrecommender.catalog.dish.extraction;

public record MenuItemText(String name, String description) {
	public MenuItemText {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Menu item name is required");
		}
		if (description != null && description.isBlank()) {
			throw new IllegalArgumentException("Menu item description must be absent instead of blank");
		}
	}

	String searchableText() {
		return description == null ? name : name + " " + description;
	}
}
