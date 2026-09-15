package com.narayansharma.foodrecommender.ml.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TrainingDatasetSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesReproducibilityMetadata() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns "
						+ "WHERE table_name = 'ML_TRAINING_DATASETS'",
				String.class);

		assertThat(columns).contains(
				"SCHEMA_VERSION",
				"FROM_INCLUSIVE",
				"CUTOFF_EXCLUSIVE",
				"OBJECT_KEY",
				"SHA256",
				"ROW_COUNT",
				"CODE_VERSION");
	}
}
