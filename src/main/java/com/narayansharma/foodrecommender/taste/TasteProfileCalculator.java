package com.narayansharma.foodrecommender.taste;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TasteProfileCalculator {
	static final String VERSION = "weighted-v1";
	private final JdbcTemplate jdbcTemplate;
	private final PreferenceScorer scorer;
	private final Clock clock;

	public TasteProfileCalculator(JdbcTemplate jdbcTemplate, PreferenceScorer scorer, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.scorer = scorer;
		this.clock = clock;
	}

	@Transactional
	public void recalculate(UUID userId) {
		lockActiveUser(userId);
		List<Evidence> evidence = cuisineEvidence(userId);
		Instant now = clock.instant();
		deleteCuisineFeatures(userId);
		Map<Feature, List<Evidence>> grouped = new LinkedHashMap<>();
		for (Evidence item : evidence) {
			grouped.computeIfAbsent(new Feature(item.key(), item.name()), ignored -> new ArrayList<>()).add(item);
		}
		for (Map.Entry<Feature, List<Evidence>> entry : grouped.entrySet()) {
			writeFeature(userId, entry.getKey(), entry.getValue(), now);
		}
		writeProfileVersion(userId, now);
	}

	private List<Evidence> cuisineEvidence(UUID userId) {
		return jdbcTemplate.query("""
				SELECT 'ONBOARDING_RESPONSE' AS source_type, response.id AS source_id,
				       cuisine.cuisine_key AS feature_key, cuisine.display_name, response.preference_score
				FROM onboarding_responses response
				JOIN onboarding_dishes onboarding ON onboarding.id = response.onboarding_dish_id
				JOIN dish_concept_cuisines mapping ON mapping.dish_concept_id = onboarding.dish_concept_id
				JOIN cuisines cuisine ON cuisine.id = mapping.cuisine_id
				WHERE response.user_id = ?
				UNION ALL
				SELECT 'RATING_REVISION' AS source_type, revision.id AS source_id,
				       cuisine.cuisine_key AS feature_key, cuisine.display_name, rating.score AS preference_score
				FROM ratings rating
				JOIN rating_revisions revision ON revision.rating_id = rating.id
				    AND revision.revision_number = (
				        SELECT MAX(latest.revision_number) FROM rating_revisions latest
				        WHERE latest.rating_id = rating.id
				    )
				JOIN dish_concept_cuisines mapping ON mapping.dish_concept_id = rating.dish_concept_id
				JOIN cuisines cuisine ON cuisine.id = mapping.cuisine_id
				WHERE rating.user_id = ? AND rating.deleted_at IS NULL
				""", (resultSet, rowNumber) -> new Evidence(
				resultSet.getString("source_type"),
				resultSet.getObject("source_id", UUID.class),
				resultSet.getString("feature_key"),
				resultSet.getString("display_name"),
				resultSet.getInt("preference_score")), userId, userId);
	}

	private void writeFeature(UUID userId, Feature feature, List<Evidence> evidence, Instant now) {
		UUID featureId = UUID.randomUUID();
		BigDecimal preference = scorer.score(evidence.stream().map(Evidence::score).toList());
		jdbcTemplate.update("""
				INSERT INTO taste_profile_features (
				    id, user_id, feature_type, feature_key, display_name,
				    preference_score, evidence_count, calculation_version, updated_at
				) VALUES (?, ?, 'CUISINE', ?, ?, ?, ?, ?, ?)
				""", featureId, userId, feature.key(), feature.name(), preference,
				evidence.size(), VERSION, Timestamp.from(now));
		for (Evidence item : evidence) {
			BigDecimal contribution = BigDecimal.valueOf(item.score() - 3).divide(new BigDecimal("2"));
			jdbcTemplate.update("""
					INSERT INTO taste_profile_evidence (
					    feature_id, source_type, source_id, contribution, created_at
					) VALUES (?, ?, ?, ?, ?)
					""", featureId, item.sourceType(), item.sourceId(), contribution, Timestamp.from(now));
		}
	}

	private void deleteCuisineFeatures(UUID userId) {
		jdbcTemplate.update("""
				DELETE FROM taste_profile_evidence
				WHERE feature_id IN (
				    SELECT id FROM taste_profile_features WHERE user_id = ? AND feature_type = 'CUISINE'
				)
				""", userId);
		jdbcTemplate.update(
				"DELETE FROM taste_profile_features WHERE user_id = ? AND feature_type = 'CUISINE'", userId);
	}

	private void writeProfileVersion(UUID userId, Instant now) {
		int updated = jdbcTemplate.update("""
				UPDATE taste_profiles SET calculation_version = ?, calculated_at = ? WHERE user_id = ?
				""", VERSION, Timestamp.from(now), userId);
		if (updated == 0) {
			jdbcTemplate.update("""
					INSERT INTO taste_profiles (user_id, calculation_version, calculated_at)
					VALUES (?, ?, ?)
					""", userId, VERSION, Timestamp.from(now));
		}
	}

	private void lockActiveUser(UUID userId) {
		List<UUID> users = jdbcTemplate.query(
				"SELECT id FROM users WHERE id = ? AND status = 'ACTIVE' FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), userId);
		if (users.isEmpty()) {
			throw new IllegalArgumentException("Unknown active user: " + userId);
		}
	}

	private record Evidence(String sourceType, UUID sourceId, String key, String name, int score) {
	}

	private record Feature(String key, String name) {
	}
}
