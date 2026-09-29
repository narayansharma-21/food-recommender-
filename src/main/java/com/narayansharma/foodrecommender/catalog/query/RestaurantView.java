package com.narayansharma.foodrecommender.catalog.query;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public record RestaurantView(
		UUID id,
		String displayName,
		URI website,
		List<RestaurantLocationView> locations) {
	public RestaurantView {
		locations = locations == null ? List.of() : List.copyOf(locations);
	}
}
