package com.narayansharma.foodrecommender.catalog.identifiers;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import java.util.UUID;

public record ResolvedRestaurantLocation(
		ExternalRestaurantId externalId,
		UUID restaurantId,
		UUID locationId) {
}
