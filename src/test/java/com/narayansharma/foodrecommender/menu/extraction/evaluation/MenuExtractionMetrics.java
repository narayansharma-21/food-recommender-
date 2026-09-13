package com.narayansharma.foodrecommender.menu.extraction.evaluation;

record MenuExtractionMetrics(
		double sectionAccuracy,
		double itemNameAccuracy,
		double priceAccuracy) {
	MenuExtractionMetrics {
		validate(sectionAccuracy);
		validate(itemNameAccuracy);
		validate(priceAccuracy);
	}

	private static void validate(double value) {
		if (!Double.isFinite(value) || value < 0 || value > 1) {
			throw new IllegalArgumentException("Extraction accuracy must be between zero and one");
		}
	}
}
