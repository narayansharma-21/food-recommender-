package com.narayansharma.foodrecommender.menu.extraction.validation;

import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuItem;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuSection;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedModifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ExtractedMenuSchemaValidator {
	private static final int MAX_SECTIONS = 50;
	private static final int MAX_ITEMS_PER_SECTION = 500;
	private static final int MAX_TOTAL_ITEMS = 2_000;
	private static final int MAX_MODIFIERS_PER_ITEM = 50;
	private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");
	private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

	public ExtractedMenu validate(ExtractedMenu menu) {
		if (menu == null) {
			throw new IllegalArgumentException("Extracted menu is required");
		}
		List<MenuSchemaViolation> violations = new ArrayList<>();
		if (menu.sections().isEmpty()) {
			violations.add(new MenuSchemaViolation("sections", "must contain at least one section"));
		}
		if (menu.sections().size() > MAX_SECTIONS) {
			violations.add(new MenuSchemaViolation("sections", "must contain at most " + MAX_SECTIONS + " sections"));
		}
		int totalItems = 0;
		for (int sectionIndex = 0; sectionIndex < menu.sections().size(); sectionIndex++) {
			ExtractedMenuSection section = menu.sections().get(sectionIndex);
			String sectionPath = "sections[" + sectionIndex + "]";
			validateRequiredText(section.name(), 200, sectionPath + ".name", violations);
			if (section.items().isEmpty()) {
				violations.add(new MenuSchemaViolation(sectionPath + ".items", "must contain at least one item"));
			}
			if (section.items().size() > MAX_ITEMS_PER_SECTION) {
				violations.add(new MenuSchemaViolation(
						sectionPath + ".items",
						"must contain at most " + MAX_ITEMS_PER_SECTION + " items"));
			}
			totalItems += section.items().size();
			validateItems(section, sectionPath, violations);
		}
		if (totalItems > MAX_TOTAL_ITEMS) {
			violations.add(new MenuSchemaViolation("sections", "must contain at most " + MAX_TOTAL_ITEMS + " total items"));
		}
		if (!violations.isEmpty()) {
			throw new MenuSchemaValidationException(violations);
		}
		return menu;
	}

	private void validateItems(
			ExtractedMenuSection section,
			String sectionPath,
			List<MenuSchemaViolation> violations) {
		for (int itemIndex = 0; itemIndex < section.items().size(); itemIndex++) {
			ExtractedMenuItem item = section.items().get(itemIndex);
			String itemPath = sectionPath + ".items[" + itemIndex + "]";
			validateRequiredText(item.name(), 300, itemPath + ".name", violations);
			validateOptionalText(item.description(), 2_000, itemPath + ".description", violations);
			validatePrice(item.price(), itemPath + ".price", violations);
			validatePriceCurrencyPair(item, itemPath, violations);
			if (item.modifiers().size() > MAX_MODIFIERS_PER_ITEM) {
				violations.add(new MenuSchemaViolation(
						itemPath + ".modifiers",
						"must contain at most " + MAX_MODIFIERS_PER_ITEM + " modifiers"));
			}
			validateModifiers(item.modifiers(), itemPath, violations);
		}
	}

	private void validateModifiers(
			List<ExtractedModifier> modifiers,
			String itemPath,
			List<MenuSchemaViolation> violations) {
		for (int modifierIndex = 0; modifierIndex < modifiers.size(); modifierIndex++) {
			ExtractedModifier modifier = modifiers.get(modifierIndex);
			String modifierPath = itemPath + ".modifiers[" + modifierIndex + "]";
			validateRequiredText(modifier.name(), 300, modifierPath + ".name", violations);
			validatePrice(modifier.price(), modifierPath + ".price", violations);
		}
	}

	private void validatePriceCurrencyPair(
			ExtractedMenuItem item,
			String itemPath,
			List<MenuSchemaViolation> violations) {
		if ((item.price() == null) != (item.currency() == null)) {
			violations.add(new MenuSchemaViolation(
					itemPath + ".currency",
					"must be present exactly when price is present"));
		} else if (item.currency() != null && !CURRENCY.matcher(item.currency()).matches()) {
			violations.add(new MenuSchemaViolation(itemPath + ".currency", "must be a three-letter uppercase code"));
		}
	}

	private void validatePrice(
			BigDecimal price,
			String path,
			List<MenuSchemaViolation> violations) {
		if (price != null && (price.signum() < 0 || price.compareTo(MAX_PRICE) > 0 || price.scale() > 2)) {
			violations.add(new MenuSchemaViolation(path, "must be non-negative, at most 99999999.99, and use at most two decimals"));
		}
	}

	private void validateRequiredText(
			String value,
			int maxLength,
			String path,
			List<MenuSchemaViolation> violations) {
		if (value.isBlank()) {
			violations.add(new MenuSchemaViolation(path, "must not be blank"));
		} else {
			validateTextShape(value, maxLength, path, violations);
		}
	}

	private void validateOptionalText(
			String value,
			int maxLength,
			String path,
			List<MenuSchemaViolation> violations) {
		if (value == null) {
			return;
		}
		if (value.isBlank()) {
			violations.add(new MenuSchemaViolation(path, "must be absent instead of blank"));
		} else {
			validateTextShape(value, maxLength, path, violations);
		}
	}

	private void validateTextShape(
			String value,
			int maxLength,
			String path,
			List<MenuSchemaViolation> violations) {
		if (!value.equals(value.strip())) {
			violations.add(new MenuSchemaViolation(path, "must not have surrounding whitespace"));
		}
		if (value.length() > maxLength) {
			violations.add(new MenuSchemaViolation(path, "must contain at most " + maxLength + " characters"));
		}
		if (value.codePoints().anyMatch(Character::isISOControl)) {
			violations.add(new MenuSchemaViolation(path, "must not contain control characters"));
		}
	}
}
