package com.narayansharma.foodrecommender.menu.extraction.structured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedMenuTextExtractorTest {
	private final RuleBasedMenuTextExtractor extractor = new RuleBasedMenuTextExtractor();

	@Test
	void extractsSectionsItemsDescriptionsPricesAndModifiers() {
		OcrResult ocrResult = result("""
				STARTERS
				Margherita Pizza  Tomato, mozzarella, basil  $14.00
				Add gluten-free crust  $3.00
				ENTREES:
				House Burger — cheddar and onions 18
				Choose fries or salad
				""");

		ExtractedMenu menu = extractor.extract(ocrResult);

		assertThat(menu.sections()).extracting(ExtractedMenuSection::name)
				.containsExactly("STARTERS", "ENTREES");
		ExtractedMenuItem pizza = menu.sections().getFirst().items().getFirst();
		assertThat(pizza.name()).isEqualTo("Margherita Pizza");
		assertThat(pizza.description()).isEqualTo("Tomato, mozzarella, basil");
		assertThat(pizza.price()).isEqualByComparingTo(new BigDecimal("14.00"));
		assertThat(pizza.currency()).isEqualTo("USD");
		assertThat(pizza.modifiers()).singleElement().satisfies(modifier -> {
			assertThat(modifier.name()).isEqualTo("Add gluten-free crust");
			assertThat(modifier.price()).isEqualByComparingTo(new BigDecimal("3.00"));
		});
		ExtractedMenuItem burger = menu.sections().get(1).items().getFirst();
		assertThat(burger.description()).isEqualTo("cheddar and onions");
		assertThat(burger.modifiers()).extracting(ExtractedModifier::name)
				.containsExactly("Choose fries or salad");
	}

	@Test
	void createsDefaultSectionAndAppendsDescriptionLines() {
		ExtractedMenu menu = extractor.extract(result("""
				Clam Chowder $9
				Cream, clams, potatoes
				"""));

		assertThat(menu.sections()).singleElement().satisfies(section -> {
			assertThat(section.name()).isEqualTo("Menu");
			assertThat(section.items()).singleElement().satisfies(item -> {
				assertThat(item.name()).isEqualTo("Clam Chowder");
				assertThat(item.description()).isEqualTo("Cream, clams, potatoes");
			});
		});
	}

	@Test
	void requiresAnOcrResult() {
		assertThatThrownBy(() -> extractor.extract(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OCR result");
	}

	private OcrResult result(String text) {
		return new OcrResult("test", "1", List.of(new OcrPage(1, text, 0.9)));
	}
}
