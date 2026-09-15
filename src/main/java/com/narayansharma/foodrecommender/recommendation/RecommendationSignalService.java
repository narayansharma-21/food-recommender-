package com.narayansharma.foodrecommender.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationSignalService {
	private static final BigDecimal PRIOR_RATING = new BigDecimal("3.5");
	private static final BigDecimal PRIOR_COUNT = new BigDecimal("5");
	private final JdbcTemplate jdbcTemplate;

	public RecommendationSignalService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public RecommendationSignals signals(UUID userId, RecommendationCandidate candidate) {
		List<Preference> preferences = preferences(userId, candidate.dishConceptId());
		int evidenceCount = preferences.stream().mapToInt(Preference::evidenceCount).sum();
		BigDecimal personal = weightedPreference(preferences, evidenceCount);
		Popularity popularity = popularity(candidate);
		return new RecommendationSignals(
				personal,
				bayesianPopularity(popularity),
				evidenceCount,
				popularity.ratingCount(),
				previouslyRated(userId, candidate));
	}

	private List<Preference> preferences(UUID userId, UUID dishConceptId) {
		if (dishConceptId == null) {
			return List.of();
		}
		return jdbcTemplate.query("""
				SELECT feature.preference_score, feature.evidence_count
				FROM taste_profile_features feature
				WHERE feature.user_id = ? AND (
				    (feature.feature_type = 'CUISINE' AND EXISTS (
				        SELECT 1 FROM dish_concept_cuisines mapping
				        JOIN cuisines value_table ON value_table.id = mapping.cuisine_id
				        WHERE mapping.dish_concept_id = ? AND value_table.cuisine_key = feature.feature_key
				    )) OR
				    (feature.feature_type = 'INGREDIENT' AND EXISTS (
				        SELECT 1 FROM dish_concept_ingredients mapping
				        JOIN ingredients value_table ON value_table.id = mapping.ingredient_id
				        WHERE mapping.dish_concept_id = ? AND value_table.ingredient_key = feature.feature_key
				    )) OR
				    (feature.feature_type = 'PREPARATION' AND EXISTS (
				        SELECT 1 FROM dish_concept_preparations mapping
				        JOIN preparations value_table ON value_table.id = mapping.preparation_id
				        WHERE mapping.dish_concept_id = ? AND value_table.preparation_key = feature.feature_key
				    )) OR
				    (feature.feature_type = 'TRAIT' AND EXISTS (
				        SELECT 1 FROM dish_concept_traits mapping
				        JOIN dish_traits value_table ON value_table.id = mapping.trait_id
				        WHERE mapping.dish_concept_id = ? AND value_table.trait_key = feature.feature_key
				    ))
				)
				""", (resultSet, rowNumber) -> new Preference(
				resultSet.getBigDecimal("preference_score"),
				resultSet.getInt("evidence_count")),
				userId, dishConceptId, dishConceptId, dishConceptId, dishConceptId);
	}

	private BigDecimal weightedPreference(List<Preference> preferences, int evidenceCount) {
		if (evidenceCount == 0) {
			return BigDecimal.ZERO.setScale(6);
		}
		BigDecimal total = preferences.stream()
				.map(preference -> preference.score().multiply(BigDecimal.valueOf(preference.evidenceCount())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return total.divide(BigDecimal.valueOf(evidenceCount), 6, RoundingMode.HALF_UP);
	}

	private Popularity popularity(RecommendationCandidate candidate) {
		String targetColumn = candidate.dishConceptId() == null ? "menu_item_id" : "dish_concept_id";
		UUID targetId = candidate.dishConceptId() == null ? candidate.menuItemId() : candidate.dishConceptId();
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*) AS rating_count, COALESCE(SUM(score), 0) AS rating_sum
				FROM ratings
				WHERE deleted_at IS NULL AND %s = ?
				""".formatted(targetColumn), (resultSet, rowNumber) -> new Popularity(
				resultSet.getInt("rating_count"),
				resultSet.getBigDecimal("rating_sum")),
				targetId);
	}

	private BigDecimal bayesianPopularity(Popularity popularity) {
		BigDecimal total = popularity.ratingSum().add(PRIOR_RATING.multiply(PRIOR_COUNT));
		BigDecimal average = total.divide(
				BigDecimal.valueOf(popularity.ratingCount()).add(PRIOR_COUNT), 8, RoundingMode.HALF_UP);
		return average.subtract(BigDecimal.ONE)
				.divide(new BigDecimal("4"), 6, RoundingMode.HALF_UP);
	}

	private boolean previouslyRated(UUID userId, RecommendationCandidate candidate) {
		String targetColumn = candidate.dishConceptId() == null ? "menu_item_id" : "dish_concept_id";
		UUID targetId = candidate.dishConceptId() == null ? candidate.menuItemId() : candidate.dishConceptId();
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM ratings
				WHERE user_id = ? AND deleted_at IS NULL AND %s = ?
				""".formatted(targetColumn), Integer.class, userId, targetId);
		return count != null && count > 0;
	}

	private record Preference(BigDecimal score, int evidenceCount) {
	}

	private record Popularity(int ratingCount, BigDecimal ratingSum) {
	}
}
