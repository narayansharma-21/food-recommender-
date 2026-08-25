package com.narayansharma.foodrecommender.catalog.discovery;

import java.math.BigDecimal;

public record RestaurantCoordinates(BigDecimal latitude, BigDecimal longitude) {
	private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
	private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
	private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
	private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

	public RestaurantCoordinates {
		if (latitude == null || longitude == null) {
			throw new IllegalArgumentException("Both restaurant coordinates are required");
		}
		if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
			throw new IllegalArgumentException("Restaurant latitude is out of range");
		}
		if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
			throw new IllegalArgumentException("Restaurant longitude is out of range");
		}
	}
}
