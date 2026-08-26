package com.narayansharma.foodrecommender.catalog.discovery;

public record RestaurantSearchQuery(
		String text,
		String city,
		String region,
		String countryCode,
		int limit,
		String cursor) {
	public RestaurantSearchQuery {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("Restaurant search text is required");
		}
		if (text.length() > 100 || text.trim().split("\\s+").length > 10) {
			throw new IllegalArgumentException("Restaurant search text is too long");
		}
		if (city == null || city.isBlank()) {
			throw new IllegalArgumentException("Restaurant search city is required");
		}
		if (city.length() > 100) {
			throw new IllegalArgumentException("Restaurant search city is too long");
		}
		if (region == null || region.isBlank() || region.length() > 100) {
			throw new IllegalArgumentException("Restaurant search region is invalid");
		}
		if (countryCode == null || !countryCode.matches("[A-Z]{2}")) {
			throw new IllegalArgumentException("Restaurant country code must be two uppercase letters");
		}
		if (limit < 1 || limit > 50) {
			throw new IllegalArgumentException("Restaurant search limit must be between 1 and 50");
		}
		if (cursor != null && cursor.length() > 100) {
			throw new IllegalArgumentException("Restaurant search cursor is too long");
		}
	}
}
