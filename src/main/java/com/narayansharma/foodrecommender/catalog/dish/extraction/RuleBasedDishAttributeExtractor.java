package com.narayansharma.foodrecommender.catalog.dish.extraction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedDishAttributeExtractor implements DishAttributeExtractor {
	private static final List<Rule> RULES = List.of(
			ingredient("clam", AttributeEvidenceType.DECLARED, 0.95, "clam", "clams"),
			ingredient("lobster", AttributeEvidenceType.DECLARED, 0.95, "lobster", "lobsters"),
			ingredient("crab", AttributeEvidenceType.DECLARED, 0.95, "crab", "crabmeat"),
			ingredient("shrimp", AttributeEvidenceType.DECLARED, 0.95, "shrimp", "prawn", "prawns"),
			ingredient("chicken", AttributeEvidenceType.DECLARED, 0.95, "chicken"),
			ingredient("beef", AttributeEvidenceType.DECLARED, 0.95, "beef"),
			ingredient("beef", AttributeEvidenceType.INFERRED, 0.85, "steak", "sirloin"),
			ingredient("pork", AttributeEvidenceType.DECLARED, 0.95, "pork"),
			ingredient("pork", AttributeEvidenceType.INFERRED, 0.85, "bacon", "prosciutto"),
			ingredient("cheese", AttributeEvidenceType.DECLARED, 0.95, "cheese"),
			ingredient("cheese", AttributeEvidenceType.INFERRED, 0.90, "cheddar", "mozzarella", "feta", "provolone"),
			ingredient("tomato", AttributeEvidenceType.DECLARED, 0.95, "tomato", "tomatoes"),
			ingredient("potato", AttributeEvidenceType.DECLARED, 0.95, "potato", "potatoes"),
			ingredient("potato", AttributeEvidenceType.INFERRED, 0.85, "fries"),
			trait("creamy", AttributeEvidenceType.DECLARED, 0.90, "creamy", "cream-based"),
			trait("crispy", AttributeEvidenceType.DECLARED, 0.90, "crispy", "crisp"),
			trait("crispy", AttributeEvidenceType.INFERRED, 0.80, "fried"),
			trait("spicy", AttributeEvidenceType.DECLARED, 0.90, "spicy"),
			trait("spicy", AttributeEvidenceType.INFERRED, 0.80, "hot peppers", "chili"),
			trait("smoky", AttributeEvidenceType.DECLARED, 0.90, "smoky"),
			trait("smoky", AttributeEvidenceType.INFERRED, 0.85, "smoked"),
			trait("sweet", AttributeEvidenceType.DECLARED, 0.90, "sweet"),
			trait("sweet", AttributeEvidenceType.INFERRED, 0.80, "honeyed"));

	@Override
	public List<DishAttributeCandidate> extract(MenuItemText item) {
		if (item == null) {
			throw new IllegalArgumentException("Menu item text is required");
		}
		String text = item.searchableText().toLowerCase(Locale.ROOT);
		Map<String, DishAttributeCandidate> candidates = new LinkedHashMap<>();
		for (Rule rule : RULES) {
			Matcher matcher = rule.pattern().matcher(text);
			if (matcher.find()) {
				DishAttributeCandidate candidate = new DishAttributeCandidate(
						rule.type(),
						rule.attributeKey(),
						rule.evidenceType(),
						rule.confidence(),
						matcher.group());
				candidates.putIfAbsent(rule.type() + ":" + rule.attributeKey(), candidate);
			}
		}
		return List.copyOf(candidates.values());
	}

	private static Rule ingredient(
			String key,
			AttributeEvidenceType evidenceType,
			double confidence,
			String... aliases) {
		return rule(DishAttributeType.INGREDIENT, key, evidenceType, confidence, aliases);
	}

	private static Rule trait(
			String key,
			AttributeEvidenceType evidenceType,
			double confidence,
			String... aliases) {
		return rule(DishAttributeType.TRAIT, key, evidenceType, confidence, aliases);
	}

	private static Rule rule(
			DishAttributeType type,
			String key,
			AttributeEvidenceType evidenceType,
			double confidence,
			String... aliases) {
		List<String> escapedAliases = new ArrayList<>();
		for (String alias : aliases) {
			escapedAliases.add(Pattern.quote(alias));
		}
		Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}_])(?:" + String.join("|", escapedAliases)
				+ ")(?![\\p{L}\\p{N}_])");
		return new Rule(type, key, evidenceType, confidence, pattern);
	}

	private record Rule(
			DishAttributeType type,
			String attributeKey,
			AttributeEvidenceType evidenceType,
			double confidence,
			Pattern pattern) {
	}
}
