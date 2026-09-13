package com.narayansharma.foodrecommender.menu.extraction.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuItem;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuSection;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedModifier;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExtractedMenuSchemaValidatorTest {
	private final ExtractedMenuSchemaValidator validator = new ExtractedMenuSchemaValidator();

	@Test
	void acceptsACompleteMenuThatMatchesTheStorageSchema() {
		ExtractedMenu menu = menu(new ExtractedMenuItem(
				"Lobster Roll",
				"Warm butter, toasted roll",
				new BigDecimal("29.00"),
				"USD",
				List.of(new ExtractedModifier("Add fries", new BigDecimal("4.00")))));

		assertThat(validator.validate(menu)).isSameAs(menu);
	}

	@Test
	void reportsEveryInvalidFieldWithItsPath() {
		ExtractedMenu menu = new ExtractedMenu(List.of(
				new ExtractedMenuSection(" ", List.of(new ExtractedMenuItem(
						" Burger ",
						" ",
						new BigDecimal("-1.001"),
						"usd",
						List.of(new ExtractedModifier("", null)))))));

		assertThatThrownBy(() -> validator.validate(menu))
				.isInstanceOfSatisfying(MenuSchemaValidationException.class, exception ->
						assertThat(exception.violations())
								.extracting(MenuSchemaViolation::path)
								.contains(
										"sections[0].name",
										"sections[0].items[0].name",
										"sections[0].items[0].description",
										"sections[0].items[0].price",
										"sections[0].items[0].currency",
										"sections[0].items[0].modifiers[0].name"));
	}

	@Test
	void requiresPriceAndCurrencyTogether() {
		ExtractedMenu menu = menu(new ExtractedMenuItem(
				"Market Fish", null, null, "USD", List.of()));

		assertThatThrownBy(() -> validator.validate(menu))
				.isInstanceOfSatisfying(MenuSchemaValidationException.class, exception ->
						assertThat(exception.violations()).singleElement().satisfies(violation ->
								assertThat(violation.path()).isEqualTo("sections[0].items[0].currency")));
	}

	@Test
	void rejectsMenusWithoutSections() {
		assertThatThrownBy(() -> validator.validate(new ExtractedMenu(List.of())))
				.isInstanceOf(MenuSchemaValidationException.class)
				.hasMessageContaining("1 violation");
	}

	private ExtractedMenu menu(ExtractedMenuItem item) {
		return new ExtractedMenu(List.of(new ExtractedMenuSection("Entrees", List.of(item))));
	}
}
