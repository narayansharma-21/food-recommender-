package com.narayansharma.foodrecommender.recommendation;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DietaryRestrictionPolicy {
	private static final Set<String> MEAT = Set.of(
			"beef", "chicken", "pork", "lamb", "turkey", "fish", "clam", "lobster", "crab", "shrimp");
	private static final Map<String, Set<String>> EXCLUDED_INGREDIENTS = Map.of(
			"vegetarian", MEAT,
			"vegan", union(MEAT, Set.of("cheese", "dairy", "egg", "honey")),
			"pescatarian", Set.of("beef", "chicken", "pork", "lamb", "turkey"));

	public boolean allows(Set<String> dietaryRestrictions, Set<String> presentIngredients) {
		if (dietaryRestrictions == null || presentIngredients == null) {
			throw new IllegalArgumentException("Dietary restrictions and ingredients are required");
		}
		for (String restriction : dietaryRestrictions) {
			Set<String> excluded = EXCLUDED_INGREDIENTS.get(restriction);
			if (excluded == null || presentIngredients.stream().anyMatch(excluded::contains)) {
				return false;
			}
		}
		return true;
	}

	private static Set<String> union(Set<String> first, Set<String> second) {
		java.util.HashSet<String> combined = new java.util.HashSet<>(first);
		combined.addAll(second);
		return Set.copyOf(combined);
	}
}
