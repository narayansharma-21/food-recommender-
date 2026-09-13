package com.narayansharma.foodrecommender.menu.extraction.structured;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedMenuTextExtractor implements MenuTextExtractor {
	private static final Pattern PRICE_AT_END = Pattern.compile(
			"^(.*?)(?:\\s+|\\.{2,})(?:USD\\s*)?\\$?(\\d{1,4}(?:[.,]\\d{2})?)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern NAME_DESCRIPTION_SEPARATOR = Pattern.compile("\\s{2,}|\\s+[—–-]\\s+");
	private static final List<String> MODIFIER_PREFIXES = List.of(
			"add ", "choose ", "choice of ", "substitute ", "swap ");

	@Override
	public ExtractedMenu extract(OcrResult ocrResult) {
		if (ocrResult == null) {
			throw new IllegalArgumentException("OCR result is required");
		}
		List<SectionBuilder> sections = new ArrayList<>();
		SectionBuilder currentSection = null;
		ItemBuilder currentItem = null;
		for (OcrPage page : ocrResult.pages()) {
			for (String rawLine : page.text().split("\\R")) {
				String line = normalize(rawLine);
				if (line.isEmpty()) {
					continue;
				}
				if (isSectionHeading(line)) {
					currentSection = new SectionBuilder(removeTrailingColon(line));
					sections.add(currentSection);
					currentItem = null;
					continue;
				}
				if (isModifier(line) && currentItem != null) {
					currentItem.modifiers.add(parseModifier(line));
					continue;
				}
				ParsedPricedText pricedText = parsePricedText(line);
				if (pricedText != null) {
					if (currentSection == null) {
						currentSection = new SectionBuilder("Menu");
						sections.add(currentSection);
					}
					currentItem = parseItem(pricedText);
					currentSection.items.add(currentItem);
				} else if (currentItem != null) {
					currentItem.appendDescription(line);
				}
			}
		}
		return new ExtractedMenu(sections.stream().map(SectionBuilder::build).toList());
	}

	private ItemBuilder parseItem(ParsedPricedText pricedText) {
		String[] fields = NAME_DESCRIPTION_SEPARATOR.split(pricedText.text(), 2);
		String description = fields.length == 2 ? fields[1].trim() : null;
		return new ItemBuilder(fields[0].trim(), description, pricedText.price());
	}

	private ExtractedModifier parseModifier(String line) {
		ParsedPricedText pricedText = parsePricedText(line);
		return pricedText == null
				? new ExtractedModifier(line, null)
				: new ExtractedModifier(pricedText.text(), pricedText.price());
	}

	private ParsedPricedText parsePricedText(String line) {
		Matcher matcher = PRICE_AT_END.matcher(line);
		if (!matcher.matches() || matcher.group(1).isBlank()) {
			return null;
		}
		return new ParsedPricedText(
				matcher.group(1).trim(),
				new BigDecimal(matcher.group(2).replace(',', '.')));
	}

	private boolean isSectionHeading(String line) {
		if (line.length() > 80 || parsePricedText(line) != null || isModifier(line)) {
			return false;
		}
		if (line.endsWith(":")) {
			return true;
		}
		String letters = line.replaceAll("[^\\p{L}]", "");
		return letters.length() >= 2 && letters.equals(letters.toUpperCase(Locale.ROOT));
	}

	private boolean isModifier(String line) {
		String lowercase = line.toLowerCase(Locale.ROOT);
		return MODIFIER_PREFIXES.stream().anyMatch(lowercase::startsWith);
	}

	private String normalize(String line) {
		return line == null ? "" : line.strip().replace('\u00a0', ' ');
	}

	private String removeTrailingColon(String line) {
		return line.endsWith(":") ? line.substring(0, line.length() - 1).strip() : line;
	}

	private record ParsedPricedText(String text, BigDecimal price) {
	}

	private static final class SectionBuilder {
		private final String name;
		private final List<ItemBuilder> items = new ArrayList<>();

		private SectionBuilder(String name) {
			this.name = name;
		}

		private ExtractedMenuSection build() {
			return new ExtractedMenuSection(name, items.stream().map(ItemBuilder::build).toList());
		}
	}

	private static final class ItemBuilder {
		private final String name;
		private String description;
		private final BigDecimal price;
		private final List<ExtractedModifier> modifiers = new ArrayList<>();

		private ItemBuilder(String name, String description, BigDecimal price) {
			this.name = name;
			this.description = description;
			this.price = price;
		}

		private void appendDescription(String line) {
			description = description == null ? line : description + " " + line;
		}

		private ExtractedMenuItem build() {
			return new ExtractedMenuItem(name, description, price, "USD", modifiers);
		}
	}
}
