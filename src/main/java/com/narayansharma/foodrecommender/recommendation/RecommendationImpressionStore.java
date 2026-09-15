package com.narayansharma.foodrecommender.recommendation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationImpressionStore {
	private final JdbcTemplate jdbcTemplate;

	public RecommendationImpressionStore(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void save(
			UUID userId,
			UUID restaurantId,
			UUID menuVersionId,
			RecommendationMode mode,
			String algorithmVersion,
			String featureVersion,
			Instant generatedAt,
			List<RankedRecommendation> results) {
		UUID requestId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO recommendation_requests (
				    id, user_id, restaurant_id, menu_version_id, mode,
				    algorithm_version, feature_version, generated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				requestId,
				userId,
				restaurantId,
				menuVersionId,
				mode.name(),
				algorithmVersion,
				featureVersion,
				Timestamp.from(generatedAt));
		for (int index = 0; index < results.size(); index++) {
			writeResult(requestId, index + 1, results.get(index));
		}
	}

	private void writeResult(UUID requestId, int rank, RankedRecommendation ranked) {
		UUID resultId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO recommendation_results (
				    id, request_id, menu_item_id, result_rank, score, confidence
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				resultId,
				requestId,
				ranked.candidate().menuItemId(),
				rank,
				ranked.score().score(),
				ranked.score().confidence());
		RecommendationSignals signals = ranked.signals();
		jdbcTemplate.update("""
				INSERT INTO recommendation_result_features (
				    result_id, personal_preference, popularity, taste_evidence_count,
				    popularity_rating_count, previously_rated
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				resultId,
				signals.personalPreference(),
				signals.popularity(),
				signals.evidenceCount(),
				signals.popularityRatingCount(),
				signals.previouslyRated());
		for (int index = 0; index < ranked.explanations().size(); index++) {
			RecommendationExplanation explanation = ranked.explanations().get(index);
			jdbcTemplate.update("""
					INSERT INTO recommendation_reasons (
					    result_id, reason_order, reason_code, explanation
					) VALUES (?, ?, ?, ?)
					""", resultId, index, explanation.code(), explanation.text());
		}
	}
}
