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
class ModelRegistryServiceTest {
	@Autowired
	private ModelRegistryService registry;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void registersAReproducibleCandidateAndFindsNoActiveModel() {
		UUID datasetId = insertDataset();

		RegisteredModel model = registry.register(registration(datasetId, "boosted-tree-v1", "{\"mae\":0.8}"));

		assertThat(model.modelVersion()).isEqualTo("boosted-tree-v1");
		assertThat(model.status()).isEqualTo("CANDIDATE");
		assertThat(model.datasetId()).isEqualTo(datasetId);
		assertThat(registry.active()).isEmpty();
	}

	@Test
	void rejectsInvalidMetricsJson() {
		UUID datasetId = insertDataset();

		assertThatThrownBy(() -> registry.register(registration(datasetId, "broken-v1", "not-json")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Model metrics JSON is invalid");
	}

	private ModelRegistration registration(UUID datasetId, String version, String metrics) {
		return new ModelRegistration(
				version,
				"BOOSTED_TREE",
				datasetId,
				"ml-models/" + version,
				"a".repeat(64),
				new BigDecimal("0.8"),
				new BigDecimal("1.0"),
				metrics,
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
