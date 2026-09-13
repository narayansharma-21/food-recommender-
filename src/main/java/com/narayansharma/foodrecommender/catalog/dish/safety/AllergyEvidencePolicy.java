package com.narayansharma.foodrecommender.catalog.dish.safety;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AllergyEvidencePolicy {
	private final JdbcTemplate jdbcTemplate;

	public AllergyEvidencePolicy(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public AllergyEvidenceAssessment assess(UUID menuItemId, Set<String> allergyIngredientKeys) {
		if (menuItemId == null || allergyIngredientKeys == null || allergyIngredientKeys.isEmpty()) {
			throw new IllegalArgumentException("Menu item and allergy ingredients are required");
		}
		requireMenuItem(menuItemId);
		Set<String> requestedKeys = normalizeKeys(allergyIngredientKeys);
		Map<String, EvidenceState> assertions = reliableAssertions(menuItemId);

		boolean containsAllergen = requestedKeys.stream()
				.anyMatch(key -> assertions.getOrDefault(key, EvidenceState.EMPTY).decision()
						== AssertionDecision.PRESENT);
		if (containsAllergen) {
			return AllergyEvidenceAssessment.CONTAINS_ALLERGEN;
		}
		boolean everyAllergenConfirmedAbsent = requestedKeys.stream()
				.allMatch(key -> assertions.getOrDefault(key, EvidenceState.EMPTY).decision()
						== AssertionDecision.ABSENT);
		return everyAllergenConfirmedAbsent
				? AllergyEvidenceAssessment.CONFIRMED_ABSENT
				: AllergyEvidenceAssessment.UNKNOWN;
	}

	private void requireMenuItem(UUID menuItemId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menu_items WHERE id = ?",
				Integer.class,
				menuItemId);
		if (count == null || count != 1) {
			throw new IllegalArgumentException("Unknown menu item: " + menuItemId);
		}
	}

	private Set<String> normalizeKeys(Set<String> keys) {
		Set<String> normalized = new HashSet<>();
		for (String key : keys) {
			if (key == null || !key.matches("[a-zA-Z][a-zA-Z0-9_-]{0,99}")) {
				throw new IllegalArgumentException("Allergy ingredient key is invalid");
			}
			normalized.add(key.toLowerCase(Locale.ROOT));
		}
		return Set.copyOf(normalized);
	}

	private Map<String, EvidenceState> reliableAssertions(UUID menuItemId) {
		List<IngredientAssertion> rows = jdbcTemplate.query("""
				SELECT ingredient.ingredient_key, evidence.assertion, evidence.evidence_type
				FROM menu_item_ingredient_evidence evidence
				JOIN ingredients ingredient ON ingredient.id = evidence.ingredient_id
				JOIN menu_extractions extraction ON extraction.id = evidence.menu_extraction_id
				WHERE evidence.menu_item_id = ?
				  AND evidence.evidence_type IN ('DECLARED', 'USER_CORRECTED')
				ORDER BY extraction.revision_number DESC, evidence.created_at DESC, evidence.id DESC
				""",
				(resultSet, rowNumber) -> new IngredientAssertion(
						resultSet.getString("ingredient_key"),
						resultSet.getString("assertion"),
						resultSet.getString("evidence_type")),
				menuItemId);
		Map<String, EvidenceState> assertions = new HashMap<>();
		for (IngredientAssertion row : rows) {
			EvidenceState state = assertions.computeIfAbsent(
					row.ingredientKey(),
					ignored -> new EvidenceState());
			state.record(row);
		}
		return assertions;
	}

	private record IngredientAssertion(String ingredientKey, String assertion, String evidenceType) {
	}

	private enum AssertionDecision {
		PRESENT,
		ABSENT,
		UNKNOWN
	}

	private static final class EvidenceState {
		private static final EvidenceState EMPTY = new EvidenceState();

		private String correctedAssertion;
		private boolean declaredPresent;
		private boolean declaredAbsent;

		void record(IngredientAssertion row) {
			if ("USER_CORRECTED".equals(row.evidenceType()) && correctedAssertion == null) {
				correctedAssertion = row.assertion();
			} else if ("PRESENT".equals(row.assertion())) {
				declaredPresent = true;
			} else {
				declaredAbsent = true;
			}
		}

		AssertionDecision decision() {
			if (correctedAssertion != null) {
				return AssertionDecision.valueOf(correctedAssertion);
			}
			if (declaredPresent) {
				return AssertionDecision.PRESENT;
			}
			return declaredAbsent ? AssertionDecision.ABSENT : AssertionDecision.UNKNOWN;
		}
	}
}
