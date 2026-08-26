package com.narayansharma.foodrecommender.catalog.discovery;

public record ExternalRestaurantId(String source, String value) {
	public ExternalRestaurantId {
		if (source == null || !source.matches("[a-z][a-z0-9_-]{0,49}")) {
			throw new IllegalArgumentException("Restaurant source is required");
		}
		if (value == null || value.isBlank() || value.length() > 255) {
			throw new IllegalArgumentException("External restaurant ID is required");
		}
	}
}
