package com.narayansharma.foodrecommender.catalog.discovery;

import java.util.List;

public record RestaurantSearchPage(List<RestaurantCandidate> restaurants, String nextCursor) {
	public RestaurantSearchPage {
		restaurants = restaurants == null ? List.of() : List.copyOf(restaurants);
	}
}
