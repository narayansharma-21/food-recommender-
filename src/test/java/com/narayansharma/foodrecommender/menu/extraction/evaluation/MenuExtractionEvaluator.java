package com.narayansharma.foodrecommender.menu.extraction.evaluation;

import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuItem;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuSection;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class MenuExtractionEvaluator {
	MenuExtractionMetrics evaluate(
			List<GreaterBostonMenuFixture> expectedFixtures,
			List<ExtractedMenu> actualMenus) {
		if (expectedFixtures.size() != actualMenus.size() || expectedFixtures.isEmpty()) {
			throw new IllegalArgumentException("Expected and actual menu sets must be non-empty and aligned");
		}
		Counts sectionCounts = new Counts();
		Counts itemCounts = new Counts();
		Counts priceCounts = new Counts();
		for (int fixtureIndex = 0; fixtureIndex < expectedFixtures.size(); fixtureIndex++) {
			evaluateMenu(
					expectedFixtures.get(fixtureIndex),
					actualMenus.get(fixtureIndex),
					sectionCounts,
					itemCounts,
					priceCounts);
		}
		return new MenuExtractionMetrics(
				sectionCounts.accuracy(),
				itemCounts.accuracy(),
				priceCounts.accuracy());
	}

	private void evaluateMenu(
			GreaterBostonMenuFixture expected,
			ExtractedMenu actual,
			Counts sectionCounts,
			Counts itemCounts,
			Counts priceCounts) {
		Map<String, ExtractedMenuSection> actualSections = new HashMap<>();
		for (ExtractedMenuSection section : actual.sections()) {
			actualSections.put(normalize(section.name()), section);
			itemCounts.actual += section.items().size();
			priceCounts.actual += section.items().size();
		}
		sectionCounts.expected += expected.expectedSections().size();
		sectionCounts.actual += actual.sections().size();
		for (GreaterBostonMenuFixture.ExpectedSection expectedSection : expected.expectedSections()) {
			ExtractedMenuSection actualSection = actualSections.get(normalize(expectedSection.name()));
			if (actualSection == null) {
				itemCounts.expected += expectedSection.items().size();
				priceCounts.expected += expectedSection.items().size();
				continue;
			}
			sectionCounts.correct++;
			evaluateItems(expectedSection, actualSection, itemCounts, priceCounts);
		}
	}

	private void evaluateItems(
			GreaterBostonMenuFixture.ExpectedSection expected,
			ExtractedMenuSection actual,
			Counts itemCounts,
			Counts priceCounts) {
		Map<String, ExtractedMenuItem> actualItems = new HashMap<>();
		for (ExtractedMenuItem item : actual.items()) {
			actualItems.put(normalize(item.name()), item);
		}
		itemCounts.expected += expected.items().size();
		priceCounts.expected += expected.items().size();
		for (GreaterBostonMenuFixture.ExpectedItem expectedItem : expected.items()) {
			ExtractedMenuItem actualItem = actualItems.get(normalize(expectedItem.name()));
			if (actualItem == null) {
				continue;
			}
			itemCounts.correct++;
			if (actualItem.price() != null
					&& actualItem.price().compareTo(new BigDecimal(expectedItem.price())) == 0) {
				priceCounts.correct++;
			}
		}
	}

	private String normalize(String value) {
		return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static final class Counts {
		private int correct;
		private int expected;
		private int actual;

		private double accuracy() {
			int denominator = Math.max(expected, actual);
			return denominator == 0 ? 1 : (double) correct / denominator;
		}
	}
}
