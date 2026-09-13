package com.narayansharma.foodrecommender.menu.extraction.evaluation;

import java.util.List;

record GreaterBostonMenuFixture(
		String id,
		String restaurant,
		String municipality,
		String sourceUrl,
		String capturedDate,
		String ocrText,
		List<ExpectedSection> expectedSections) {
	record ExpectedSection(String name, List<ExpectedItem> items) {
	}

	record ExpectedItem(String name, String price) {
	}
}
