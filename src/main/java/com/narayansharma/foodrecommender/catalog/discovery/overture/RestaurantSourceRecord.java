package com.narayansharma.foodrecommender.catalog.discovery.overture;

import java.math.BigDecimal;
import java.time.Instant;

record RestaurantSourceRecord(
		String externalId,
		String displayName,
		String normalizedName,
		String addressLine1,
		String addressLine2,
		String city,
		String region,
		String postalCode,
		String countryCode,
		BigDecimal latitude,
		BigDecimal longitude,
		String phone,
		String websiteUrl,
		String category,
		BigDecimal confidence,
		Instant importedAt) {
}
