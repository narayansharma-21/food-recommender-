package com.narayansharma.foodrecommender.menu.extraction.structured;

import com.narayansharma.foodrecommender.menu.extraction.evidence.AttributedExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.evidence.FieldEvidence;
import com.narayansharma.foodrecommender.menu.extraction.evidence.FieldProvenance;
import com.narayansharma.foodrecommender.menu.extraction.evidence.ProvenanceSource;
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
	private static final double SECTION_CONFIDENCE_FACTOR = 0.90;
	private static final double ITEM_NAME_CONFIDENCE_FACTOR = 0.85;
	private static final double DESCRIPTION_CONFIDENCE_FACTOR = 0.80;
	private static final double PRICE_CONFIDENCE_FACTOR = 0.90;
	private static final double MODIFIER_CONFIDENCE_FACTOR = 0.80;
	private static final double INFERRED_CURRENCY_CONFIDENCE = 0.70;
	private static final Pattern PRICE_AT_END = Pattern.compile(
			"^(.*?)(?:\\s+|\\.{2,})(?:USD\\s*)?\\$?(\\d{1,4}(?:[.,]\\d{2})?)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern NAME_DESCRIPTION_SEPARATOR = Pattern.compile("\\s{2,}|\\s+[—–-]\\s+");
	private static final List<String> MODIFIER_PREFIXES = List.of(
			"add ", "choose ", "choice of ", "substitute ", "swap ");

	@Override
	public AttributedExtractedMenu extractWithEvidence(OcrResult ocrResult) {
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
					currentSection = new SectionBuilder(
							removeTrailingColon(line),
							evidence(page, line, SECTION_CONFIDENCE_FACTOR));
					sections.add(currentSection);
					currentItem = null;
					continue;
				}
				if (isModifier(line) && currentItem != null) {
					currentItem.modifiers.add(parseModifier(page, line));
					continue;
				}
				ParsedPricedText pricedText = parsePricedText(line);
				if (pricedText != null) {
					if (currentSection == null) {
						currentSection = new SectionBuilder(
								"Menu",
								inferredEvidence("Default section added because the menu had no heading", 0.65));
						sections.add(currentSection);
					}
					currentItem = parseItem(page, line, pricedText);
					currentSection.items.add(currentItem);
				} else if (currentItem != null) {
					currentItem.appendDescription(line, evidence(page, line, DESCRIPTION_CONFIDENCE_FACTOR));
				}
			}
		}
		List<FieldEvidence> fieldEvidence = new ArrayList<>();
		List<ExtractedMenuSection> extractedSections = new ArrayList<>();
		for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
			extractedSections.add(sections.get(sectionIndex).build(sectionIndex, fieldEvidence));
		}
		return new AttributedExtractedMenu(new ExtractedMenu(extractedSections), fieldEvidence);
	}

	private ItemBuilder parseItem(OcrPage page, String line, ParsedPricedText pricedText) {
		String[] fields = NAME_DESCRIPTION_SEPARATOR.split(pricedText.text(), 2);
		String description = fields.length == 2 ? fields[1].trim() : null;
		EvidenceSeed source = evidence(page, line, ITEM_NAME_CONFIDENCE_FACTOR);
		return new ItemBuilder(
				fields[0].trim(),
				description,
				pricedText.price(),
				source,
				description == null ? new ArrayList<>() : new ArrayList<>(List.of(evidence(page, line, DESCRIPTION_CONFIDENCE_FACTOR))),
				evidence(page, line, PRICE_CONFIDENCE_FACTOR));
	}

	private ModifierBuilder parseModifier(OcrPage page, String line) {
		ParsedPricedText pricedText = parsePricedText(line);
		EvidenceSeed source = evidence(page, line, MODIFIER_CONFIDENCE_FACTOR);
		return pricedText == null
				? new ModifierBuilder(line, null, source, null)
				: new ModifierBuilder(pricedText.text(), pricedText.price(), source, evidence(page, line, PRICE_CONFIDENCE_FACTOR));
	}

	private EvidenceSeed evidence(OcrPage page, String sourceText, double confidenceFactor) {
		return new EvidenceSeed(
				page.confidence() * confidenceFactor,
				new FieldProvenance(ProvenanceSource.OCR_TEXT, page.pageNumber(), truncate(sourceText)));
	}

	private EvidenceSeed inferredEvidence(String explanation, double confidence) {
		return new EvidenceSeed(
				confidence,
				new FieldProvenance(ProvenanceSource.INFERRED, null, explanation));
	}

	private String truncate(String value) {
		return value.length() <= 500 ? value : value.substring(0, 500);
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

	private record EvidenceSeed(double confidence, FieldProvenance provenance) {
	}

	private static final class SectionBuilder {
		private final String name;
		private final EvidenceSeed nameEvidence;
		private final List<ItemBuilder> items = new ArrayList<>();

		private SectionBuilder(String name, EvidenceSeed nameEvidence) {
			this.name = name;
			this.nameEvidence = nameEvidence;
		}

		private ExtractedMenuSection build(int sectionIndex, List<FieldEvidence> fields) {
			String sectionPath = "sections[" + sectionIndex + "]";
			fields.add(field(sectionPath + ".name", nameEvidence));
			List<ExtractedMenuItem> extractedItems = new ArrayList<>();
			for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
				extractedItems.add(items.get(itemIndex).build(sectionPath, itemIndex, fields));
			}
			return new ExtractedMenuSection(name, extractedItems);
		}
	}

	private static final class ItemBuilder {
		private final String name;
		private String description;
		private final BigDecimal price;
		private final EvidenceSeed nameEvidence;
		private final List<EvidenceSeed> descriptionEvidence;
		private final EvidenceSeed priceEvidence;
		private final List<ModifierBuilder> modifiers = new ArrayList<>();

		private ItemBuilder(
				String name,
				String description,
				BigDecimal price,
				EvidenceSeed nameEvidence,
				List<EvidenceSeed> descriptionEvidence,
				EvidenceSeed priceEvidence) {
			this.name = name;
			this.description = description;
			this.price = price;
			this.nameEvidence = nameEvidence;
			this.descriptionEvidence = descriptionEvidence;
			this.priceEvidence = priceEvidence;
		}

		private void appendDescription(String line, EvidenceSeed evidence) {
			description = description == null ? line : description + " " + line;
			descriptionEvidence.add(evidence);
		}

		private ExtractedMenuItem build(String sectionPath, int itemIndex, List<FieldEvidence> fields) {
			String itemPath = sectionPath + ".items[" + itemIndex + "]";
			fields.add(field(itemPath + ".name", nameEvidence));
			if (description != null) {
				fields.add(field(itemPath + ".description", descriptionEvidence));
			}
			fields.add(field(itemPath + ".price", priceEvidence));
			fields.add(field(
					itemPath + ".currency",
					new EvidenceSeed(
							INFERRED_CURRENCY_CONFIDENCE,
							new FieldProvenance(ProvenanceSource.INFERRED, null, "USD is the launch-market default"))));
			List<ExtractedModifier> extractedModifiers = new ArrayList<>();
			for (int modifierIndex = 0; modifierIndex < modifiers.size(); modifierIndex++) {
				extractedModifiers.add(modifiers.get(modifierIndex).build(itemPath, modifierIndex, fields));
			}
			return new ExtractedMenuItem(name, description, price, "USD", extractedModifiers);
		}
	}

	private static final class ModifierBuilder {
		private final String name;
		private final BigDecimal price;
		private final EvidenceSeed nameEvidence;
		private final EvidenceSeed priceEvidence;

		private ModifierBuilder(
				String name,
				BigDecimal price,
				EvidenceSeed nameEvidence,
				EvidenceSeed priceEvidence) {
			this.name = name;
			this.price = price;
			this.nameEvidence = nameEvidence;
			this.priceEvidence = priceEvidence;
		}

		private ExtractedModifier build(String itemPath, int modifierIndex, List<FieldEvidence> fields) {
			String modifierPath = itemPath + ".modifiers[" + modifierIndex + "]";
			fields.add(field(modifierPath + ".name", nameEvidence));
			if (priceEvidence != null) {
				fields.add(field(modifierPath + ".price", priceEvidence));
			}
			return new ExtractedModifier(name, price);
		}
	}

	private static FieldEvidence field(String path, EvidenceSeed evidence) {
		return new FieldEvidence(path, evidence.confidence(), List.of(evidence.provenance()));
	}

	private static FieldEvidence field(String path, List<EvidenceSeed> evidence) {
		double confidence = evidence.stream().mapToDouble(EvidenceSeed::confidence).min().orElse(0);
		return new FieldEvidence(path, confidence, evidence.stream().map(EvidenceSeed::provenance).toList());
	}
}
