package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class RecommendationFeatureSnapshotSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesPointInTimeRankingFeatures() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns "
						+ "WHERE table_name = 'RECOMMENDATION_RESULT_FEATURES'",
				String.class);

		assertThat(columns).contains(
				"RESULT_ID",
				"PERSONAL_PREFERENCE",
				"POPULARITY",
				"TASTE_EVIDENCE_COUNT",
				"POPULARITY_RATING_COUNT",
				"PREVIOUSLY_RATED");
	}
}
