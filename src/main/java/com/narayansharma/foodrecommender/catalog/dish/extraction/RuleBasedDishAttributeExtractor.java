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
			ingredient("clam", 0.95, "clam", "clams"),
			ingredient("lobster", 0.95, "lobster", "lobsters"),
			ingredient("crab", 0.95, "crab", "crabmeat"),
			ingredient("shrimp", 0.95, "shrimp", "prawn", "prawns"),
			ingredient("chicken", 0.95, "chicken"),
			ingredient("beef", 0.90, "beef", "steak", "sirloin"),
			ingredient("pork", 0.85, "pork", "bacon", "prosciutto"),
			ingredient("cheese", 0.90, "cheese", "cheddar", "mozzarella", "feta", "provolone"),
			ingredient("tomato", 0.95, "tomato", "tomatoes"),
			ingredient("potato", 0.95, "potato", "potatoes", "fries"),
			trait("creamy", 0.90, "creamy", "cream-based"),
			trait("crispy", 0.90, "crispy", "crisp", "fried"),
			trait("spicy", 0.90, "spicy", "hot peppers", "chili"),
			trait("smoky", 0.90, "smoky", "smoked"),
			trait("sweet", 0.85, "sweet", "honeyed"));

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
						rule.confidence(),
						matcher.group());
				candidates.put(rule.type() + ":" + rule.attributeKey(), candidate);
			}
		}
		return List.copyOf(candidates.values());
	}

	private static Rule ingredient(String key, double confidence, String... aliases) {
		return rule(DishAttributeType.INGREDIENT, key, confidence, aliases);
	}

	private static Rule trait(String key, double confidence, String... aliases) {
		return rule(DishAttributeType.TRAIT, key, confidence, aliases);
	}

	private static Rule rule(
			DishAttributeType type,
			String key,
			double confidence,
			String... aliases) {
		List<String> escapedAliases = new ArrayList<>();
		for (String alias : aliases) {
			escapedAliases.add(Pattern.quote(alias));
		}
		Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}_])(?:" + String.join("|", escapedAliases)
				+ ")(?![\\p{L}\\p{N}_])");
		return new Rule(type, key, confidence, pattern);
	}

	private record Rule(
			DishAttributeType type,
			String attributeKey,
			double confidence,
			Pattern pattern) {
	}
}
