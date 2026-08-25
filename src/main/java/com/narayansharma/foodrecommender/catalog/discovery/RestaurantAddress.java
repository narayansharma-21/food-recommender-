package com.narayansharma.foodrecommender.catalog.discovery;

public record RestaurantAddress(
		String addressLine1,
		String addressLine2,
		String city,
		String region,
		String postalCode,
		String countryCode) {
}
