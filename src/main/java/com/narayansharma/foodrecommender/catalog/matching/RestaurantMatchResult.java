package com.narayansharma.foodrecommender.catalog.matching;

import java.util.List;

public record RestaurantMatchResult(
		RestaurantMatchLevel level,
		KnownRestaurantLocation location,
		double score,
		List<String> reasons) {
	public RestaurantMatchResult {
		if (level == null || !Double.isFinite(score) || score < 0 || score > 1) {
			throw new IllegalArgumentException("Restaurant match result is invalid");
		}
		if (level != RestaurantMatchLevel.NONE && location == null) {
			throw new IllegalArgumentException("A matched location is required");
		}
		reasons = reasons == null ? List.of() : List.copyOf(reasons);
	}

	public static RestaurantMatchResult none() {
		return new RestaurantMatchResult(RestaurantMatchLevel.NONE, null, 0, List.of());
	}
}
