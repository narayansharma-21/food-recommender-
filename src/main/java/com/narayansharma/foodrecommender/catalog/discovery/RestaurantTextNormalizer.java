package com.narayansharma.foodrecommender.catalog.discovery;

import java.text.Normalizer;
import java.util.Locale;

public final class RestaurantTextNormalizer {
	private RestaurantTextNormalizer() {
	}

	public static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value, Normalizer.Form.NFKD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ")
				.trim();
	}
}
