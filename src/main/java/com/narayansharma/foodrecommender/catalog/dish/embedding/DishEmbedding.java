package com.narayansharma.foodrecommender.catalog.dish.embedding;

import java.util.Arrays;

public record DishEmbedding(
		String provider,
		String modelVersion,
		double[] vector) {
	public DishEmbedding {
		if (provider == null
				|| !provider.matches("[a-z][a-z0-9_-]{0,49}")
				|| modelVersion == null
				|| modelVersion.isBlank()
				|| modelVersion.length() > 100
				|| vector == null
				|| vector.length < 16
				|| vector.length > 2_048
				|| Arrays.stream(vector).anyMatch(value -> !Double.isFinite(value))) {
			throw new IllegalArgumentException("Dish embedding is invalid");
		}
		vector = vector.clone();
	}

	@Override
	public double[] vector() {
		return vector.clone();
	}
}
