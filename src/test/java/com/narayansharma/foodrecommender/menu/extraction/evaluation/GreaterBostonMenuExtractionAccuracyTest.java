package com.narayansharma.foodrecommender.menu.extraction.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuItem;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuSection;
import com.narayansharma.foodrecommender.menu.extraction.structured.RuleBasedMenuTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GreaterBostonMenuExtractionAccuracyTest {
	private static final double MINIMUM_ACCURACY = 0.90;

	@Test
	void meetsTheProofOfConceptAccuracyFloor() throws IOException {
		List<GreaterBostonMenuFixture> fixtures = GreaterBostonMenuFixtureTest.loadFixtures();
		RuleBasedMenuTextExtractor extractor = new RuleBasedMenuTextExtractor();
		List<ExtractedMenu> actualMenus = new ArrayList<>();
		for (GreaterBostonMenuFixture fixture : fixtures) {
			OcrResult ocrResult = new OcrResult(
					"fixture",
					"1",
					List.of(new OcrPage(1, fixture.ocrText(), 1)));
			actualMenus.add(extractor.extract(ocrResult));
		}

		MenuExtractionMetrics metrics = new MenuExtractionEvaluator().evaluate(fixtures, actualMenus);

		assertThat(metrics.sectionAccuracy())
				.as("section extraction accuracy")
				.isGreaterThanOrEqualTo(MINIMUM_ACCURACY);
		assertThat(metrics.itemNameAccuracy())
				.as("item-name extraction accuracy")
				.isGreaterThanOrEqualTo(MINIMUM_ACCURACY);
		assertThat(metrics.priceAccuracy())
				.as("price extraction accuracy")
				.isGreaterThanOrEqualTo(MINIMUM_ACCURACY);
	}

	@Test
	void penalizesExtraItemsInsteadOfReportingRecallOnly() {
		GreaterBostonMenuFixture fixture = new GreaterBostonMenuFixture(
				"test",
				"Test Restaurant",
				"Boston",
				"https://example.com/menu",
				"2026-09-12",
				"unused",
				List.of(new GreaterBostonMenuFixture.ExpectedSection(
						"Entrees",
						List.of(new GreaterBostonMenuFixture.ExpectedItem("Burger", "12.00")))));
		ExtractedMenu actual = new ExtractedMenu(List.of(new ExtractedMenuSection(
				"Entrees",
				List.of(
						new ExtractedMenuItem("Burger", null, new BigDecimal("12"), "USD", List.of()),
						new ExtractedMenuItem("Extra", null, new BigDecimal("8"), "USD", List.of())))));

		MenuExtractionMetrics metrics = new MenuExtractionEvaluator().evaluate(List.of(fixture), List.of(actual));

		assertThat(metrics.sectionAccuracy()).isEqualTo(1);
		assertThat(metrics.itemNameAccuracy()).isEqualTo(0.5);
		assertThat(metrics.priceAccuracy()).isEqualTo(0.5);
	}
}
