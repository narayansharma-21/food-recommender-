package com.narayansharma.foodrecommender.ml.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ModelRegistrySchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesReproducibleModelsAndPromotionHistory() {
		List<String> modelColumns = columns("ML_MODEL_VERSIONS");
		List<String> promotionColumns = columns("ML_MODEL_PROMOTIONS");

		assertThat(modelColumns).contains(
				"DATASET_ID", "ARTIFACT_SHA256", "OVERALL_MAE", "BASELINE_MAE", "CODE_VERSION", "STATUS");
		assertThat(promotionColumns).contains("FROM_MODEL_ID", "TO_MODEL_ID", "PROMOTED_BY", "REASON");
	}

	private List<String> columns(String table) {
		return jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_name = ?",
				String.class,
				table);
	}
}
