package com.narayansharma.foodrecommender.catalog.query;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import java.net.URI;
import java.util.UUID;

public record RestaurantSearchResult(
		UUID restaurantId,
		UUID locationId,
		ExternalRestaurantId externalId,
		String displayName,
		RestaurantAddress address,
		RestaurantCoordinates coordinates,
		String phone,
		URI website) {
}
