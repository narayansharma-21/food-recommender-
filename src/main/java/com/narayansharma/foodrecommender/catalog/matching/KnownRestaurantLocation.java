package com.narayansharma.foodrecommender.catalog.matching;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import java.util.List;
import java.util.UUID;

public record KnownRestaurantLocation(
		UUID restaurantId,
		UUID locationId,
		String displayName,
		RestaurantAddress address,
		RestaurantCoordinates coordinates,
		String phone,
		List<ExternalRestaurantId> externalIds) {
	public KnownRestaurantLocation {
		if (restaurantId == null || locationId == null) {
			throw new IllegalArgumentException("Restaurant and location IDs are required");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Restaurant name is required");
		}
		externalIds = externalIds == null ? List.of() : List.copyOf(externalIds);
	}
}
