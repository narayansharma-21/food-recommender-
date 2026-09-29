package com.narayansharma.foodrecommender.catalog.query;

import java.util.List;

public record RestaurantSearchResponse(
		List<RestaurantSearchResult> restaurants,
		String nextCursor) {
	public RestaurantSearchResponse {
		restaurants = restaurants == null ? List.of() : List.copyOf(restaurants);
	}
}
