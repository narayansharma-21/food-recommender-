package com.narayansharma.foodrecommender.catalog.discovery;

import java.net.URI;

public record RestaurantCandidate(
		ExternalRestaurantId externalId,
		String displayName,
		RestaurantAddress address,
		RestaurantCoordinates coordinates,
		String phone,
		URI website) {
	public RestaurantCandidate {
		if (externalId == null) {
			throw new IllegalArgumentException("External restaurant ID is required");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Restaurant name is required");
		}
	}
}
