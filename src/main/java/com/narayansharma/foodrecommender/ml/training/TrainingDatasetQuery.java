package com.narayansharma.foodrecommender.ml.training;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrainingDatasetQuery {
	public static final String SCHEMA_VERSION = "recommendation-training-example-v1";
	private final JdbcTemplate jdbcTemplate;

	public TrainingDatasetQuery(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<TrainingExample> examples(Instant fromInclusive, Instant cutoffExclusive) {
		if (fromInclusive == null || cutoffExclusive == null || !fromInclusive.isBefore(cutoffExclusive)) {
			throw new IllegalArgumentException("Training dataset time range is invalid");
		}
		return jdbcTemplate.query("""
				WITH attributed_ratings AS (
				    SELECT result.id AS recommendation_result_id,
				           rating.id AS rating_id,
				           request.user_id,
				           request.restaurant_id,
				           result.menu_item_id,
				           item.dish_concept_id,
				           request.generated_at AS recommended_at,
				           revision.created_at AS rated_at,
				           request.mode,
				           request.algorithm_version,
				           request.feature_version,
				           result.result_rank,
				           feature.personal_preference,
				           feature.popularity,
				           feature.taste_evidence_count,
				           feature.popularity_rating_count,
				           feature.previously_rated,
				           revision.score AS rating_score,
				           revision.would_order_again,
				           ROW_NUMBER() OVER (
				               PARTITION BY rating.id
				               ORDER BY request.generated_at DESC, result.id
				           ) AS attribution_rank
				    FROM rating_revisions revision
				    JOIN ratings rating ON rating.id = revision.rating_id
				    JOIN recommendation_requests request
				      ON request.user_id = rating.user_id
				     AND request.generated_at <= revision.created_at
				    JOIN recommendation_results result
				      ON result.request_id = request.id
				     AND result.menu_item_id = rating.menu_item_id
				    JOIN recommendation_result_features feature ON feature.result_id = result.id
				    JOIN menu_items item ON item.id = result.menu_item_id
				    WHERE revision.change_type = 'CREATED'
				      AND rating.deleted_at IS NULL
				      AND request.generated_at >= ?
				      AND revision.created_at < ?
				)
				SELECT *
				FROM attributed_ratings
				WHERE attribution_rank = 1
				ORDER BY rated_at, rating_id
				""", (resultSet, rowNumber) -> new TrainingExample(
				SCHEMA_VERSION,
				resultSet.getObject("recommendation_result_id", UUID.class),
				resultSet.getObject("rating_id", UUID.class),
				resultSet.getObject("user_id", UUID.class),
				resultSet.getObject("restaurant_id", UUID.class),
				resultSet.getObject("menu_item_id", UUID.class),
				resultSet.getObject("dish_concept_id", UUID.class),
				resultSet.getTimestamp("recommended_at").toInstant(),
				resultSet.getTimestamp("rated_at").toInstant(),
				resultSet.getString("mode"),
				resultSet.getString("algorithm_version"),
				resultSet.getString("feature_version"),
				resultSet.getInt("result_rank"),
				resultSet.getBigDecimal("personal_preference"),
				resultSet.getBigDecimal("popularity"),
				resultSet.getInt("taste_evidence_count"),
				resultSet.getInt("popularity_rating_count"),
				resultSet.getBoolean("previously_rated"),
				resultSet.getInt("rating_score"),
				(Boolean) resultSet.getObject("would_order_again")),
				Timestamp.from(fromInclusive),
				Timestamp.from(cutoffExclusive));
	}
}
