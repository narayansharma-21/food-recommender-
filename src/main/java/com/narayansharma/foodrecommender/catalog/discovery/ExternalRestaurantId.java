package com.narayansharma.foodrecommender.catalog.discovery;

public record ExternalRestaurantId(String source, String value) {
	public ExternalRestaurantId {
		if (source == null || source.isBlank()) {
			throw new IllegalArgumentException("Restaurant source is required");
		}
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("External restaurant ID is required");
		}
	}
}
