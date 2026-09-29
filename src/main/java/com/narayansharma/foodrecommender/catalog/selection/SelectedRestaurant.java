package com.narayansharma.foodrecommender.catalog.selection;

import com.narayansharma.foodrecommender.catalog.matching.RestaurantMatchLevel;
import java.util.UUID;

public record SelectedRestaurant(
		UUID restaurantId,
		UUID locationId,
		boolean created,
		RestaurantMatchLevel matchLevel,
		boolean duplicateReviewRequired) {
}
