package com.narayansharma.foodrecommender.catalog.discovery;

public interface RestaurantSearchSource {
	RestaurantSearchPage search(RestaurantSearchQuery query);
}
