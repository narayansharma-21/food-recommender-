package com.narayansharma.foodrecommender.catalog.query;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import java.util.UUID;

public record RestaurantLocationView(
		UUID id,
		RestaurantAddress address,
		RestaurantCoordinates coordinates,
		String phone,
		String timezone) {
}
