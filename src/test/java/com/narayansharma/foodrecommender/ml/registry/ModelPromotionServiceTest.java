package com.narayansharma.foodrecommender.ml.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ModelPromotionServiceTest {
	@Autowired
	private ModelRegistryService registry;

	@Autowired
	private ModelPromotionService promotionService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void promotesReplacesAndRollsBackModelsWithHistory() {
		UUID datasetId = insertDataset();
		RegisteredModel first = registry.register(registration(datasetId, "tree-v1", "0.8"));
		RegisteredModel second = registry.register(registration(datasetId, "tree-v2", "0.7"));

		promotionService.promote(first.id(), "admin", "Initial model");
		promotionService.promote(second.id(), "admin", "Lower error");
		RegisteredModel rolledBack = promotionService.promote(first.id(), "admin", "Rollback test");

		assertThat(rolledBack.status()).isEqualTo("ACTIVE");
		assertThat(registry.get(second.id()).status()).isEqualTo("RETIRED");
		assertThat(registry.active()).contains(rolledBack);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM ml_model_promotions", Integer.class)).isEqualTo(3);
	}

	@Test
	void blocksAModelThatDoesNotBeatItsBaseline() {
		UUID datasetId = insertDataset();
		RegisteredModel model = registry.register(registration(datasetId, "tree-worse", "1.0"));

		assertThatThrownBy(() -> promotionService.promote(model.id(), "admin", "Should fail"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Model must beat its baseline before promotion");
		assertThat(registry.active()).isEmpty();
	}

	private ModelRegistration registration(UUID datasetId, String version, String mae) {
		return new ModelRegistration(
				version,
				"BOOSTED_TREE",
				datasetId,
				"ml-models/" + version,
				"a".repeat(64),
				new BigDecimal(mae),
				BigDecimal.ONE,
				"{\"overallMae\":" + mae + "}",
				"git-test");
	}

	private UUID insertDataset() {
		UUID datasetId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO ml_training_datasets (
				    id, schema_version, from_inclusive, cutoff_exclusive, object_key,
				    sha256, row_count, code_version, created_at
				) VALUES (?, 'recommendation-training-example-v1', ?, ?, ?, ?, 10, 'git-test', ?)
				""",
				datasetId,
				Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
				Timestamp.from(Instant.parse("2026-02-01T00:00:00Z")),
				"ml-datasets/" + datasetId,
				"b".repeat(64),
				Timestamp.from(Instant.parse("2026-02-01T00:00:00Z")));
		return datasetId;
	}
}
