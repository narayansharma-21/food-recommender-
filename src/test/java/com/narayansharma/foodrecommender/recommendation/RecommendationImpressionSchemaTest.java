package com.narayansharma.foodrecommender.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class RecommendationImpressionSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void recordsAlgorithmAndFeatureVersions() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns "
						+ "WHERE table_name = 'RECOMMENDATION_REQUESTS'",
				String.class);

		assertThat(columns).contains("ALGORITHM_VERSION", "FEATURE_VERSION");
	}
}
