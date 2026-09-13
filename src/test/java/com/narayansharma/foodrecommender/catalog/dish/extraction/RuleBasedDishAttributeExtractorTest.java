package com.narayansharma.foodrecommender.catalog.dish.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RuleBasedDishAttributeExtractorTest {
	private final RuleBasedDishAttributeExtractor extractor = new RuleBasedDishAttributeExtractor();

	@Test
	void extractsLikelyIngredientsAndTraitsWithMatchedText() {
		var candidates = extractor.extract(new MenuItemText(
				"Creamy New England Clam Chowder",
				"Clams, potatoes, and smoked bacon"));

		assertThat(candidates)
				.filteredOn(candidate -> candidate.type() == DishAttributeType.INGREDIENT)
				.extracting(DishAttributeCandidate::attributeKey)
				.containsExactly("clam", "pork", "potato");
		assertThat(candidates)
				.filteredOn(candidate -> candidate.type() == DishAttributeType.TRAIT)
				.extracting(DishAttributeCandidate::attributeKey)
				.containsExactly("creamy", "smoky");
		assertThat(candidates)
				.filteredOn(candidate -> candidate.attributeKey().equals("pork"))
				.singleElement()
				.satisfies(candidate -> {
					assertThat(candidate.matchedText()).isEqualTo("bacon");
					assertThat(candidate.confidence()).isEqualTo(0.85);
				});
	}

	@Test
	void usesWholeWordsAndReturnsEachAttributeOnlyOnce() {
		var candidates = extractor.extract(new MenuItemText(
				"Shrimp with shrimp sauce",
				"Crabapple glaze"));

		assertThat(candidates).extracting(DishAttributeCandidate::attributeKey)
				.containsExactly("shrimp");
	}

	@Test
	void requiresMenuItemText() {
		assertThatThrownBy(() -> extractor.extract(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("required");
	}
}
