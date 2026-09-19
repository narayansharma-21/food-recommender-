package com.narayansharma.foodrecommender.ml.registry;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ModelRegistryService {
	private static final Set<String> MODEL_TYPES = Set.of("GLOBAL_MEAN", "BOOSTED_TREE");
	private static final Pattern VERSION = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}");
	private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public ModelRegistryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional
	public RegisteredModel register(ModelRegistration registration) {
		validate(registration);
		requireDataset(registration.datasetId());
		UUID id = UUID.randomUUID();
		Instant createdAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
		jdbcTemplate.update("""
				INSERT INTO ml_model_versions (
				    id, model_version, model_type, dataset_id, artifact_object_key,
				    artifact_sha256, overall_mae, baseline_mae, metrics_json,
				    code_version, status, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CANDIDATE', ?)
				""",
				id,
				registration.modelVersion(),
				registration.modelType(),
				registration.datasetId(),
				registration.artifactObjectKey(),
				registration.artifactSha256(),
				registration.overallMae(),
				registration.baselineMae(),
				registration.metricsJson(),
				registration.codeVersion(),
				Timestamp.from(createdAt));
		return get(id);
	}

	public RegisteredModel get(UUID id) {
		if (id == null) {
			throw new IllegalArgumentException("Model ID is required");
		}
		List<RegisteredModel> models = jdbcTemplate.query(
				"SELECT * FROM ml_model_versions WHERE id = ?",
				(resultSet, rowNumber) -> map(resultSet),
				id);
		if (models.isEmpty()) {
			throw new IllegalArgumentException("Unknown model: " + id);
		}
		return models.getFirst();
	}

	public Optional<RegisteredModel> active() {
		List<RegisteredModel> models = jdbcTemplate.query(
				"SELECT * FROM ml_model_versions WHERE status = 'ACTIVE' ORDER BY promoted_at DESC, id",
				(resultSet, rowNumber) -> map(resultSet));
		if (models.size() > 1) {
			throw new IllegalStateException("More than one ML model is active");
		}
		return models.stream().findFirst();
	}

	private RegisteredModel map(java.sql.ResultSet resultSet) throws java.sql.SQLException {
		Timestamp promotedAt = resultSet.getTimestamp("promoted_at");
		Timestamp retiredAt = resultSet.getTimestamp("retired_at");
		return new RegisteredModel(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("model_version"),
				resultSet.getString("model_type"),
				resultSet.getObject("dataset_id", UUID.class),
				resultSet.getString("artifact_object_key"),
				resultSet.getString("artifact_sha256"),
				resultSet.getBigDecimal("overall_mae"),
				resultSet.getBigDecimal("baseline_mae"),
				resultSet.getString("code_version"),
				resultSet.getString("status"),
				resultSet.getTimestamp("created_at").toInstant(),
				promotedAt == null ? null : promotedAt.toInstant(),
				retiredAt == null ? null : retiredAt.toInstant());
	}

	private void validate(ModelRegistration registration) {
		if (registration == null
				|| registration.datasetId() == null
				|| registration.modelVersion() == null
				|| !VERSION.matcher(registration.modelVersion()).matches()
				|| !MODEL_TYPES.contains(registration.modelType())) {
			throw new IllegalArgumentException("Model registration identity is invalid");
		}
		if (registration.artifactObjectKey() == null
				|| registration.artifactObjectKey().isBlank()
				|| registration.artifactObjectKey().length() > 500
				|| registration.artifactSha256() == null
				|| !SHA256.matcher(registration.artifactSha256()).matches()) {
			throw new IllegalArgumentException("Model artifact reference is invalid");
		}
		validateMetrics(registration);
		if (registration.codeVersion() == null
				|| registration.codeVersion().isBlank()
				|| registration.codeVersion().length() > 100) {
			throw new IllegalArgumentException("Model code version is invalid");
		}
	}

	private void validateMetrics(ModelRegistration registration) {
		BigDecimal overall = registration.overallMae();
		BigDecimal baseline = registration.baselineMae();
		if (overall == null || baseline == null || overall.signum() < 0 || baseline.signum() < 0) {
			throw new IllegalArgumentException("Model metrics are invalid");
		}
		String metrics = registration.metricsJson();
		if (metrics == null || metrics.isBlank() || metrics.length() > 8000) {
			throw new IllegalArgumentException("Model metrics JSON is invalid");
		}
		try {
			if (!objectMapper.readTree(metrics).isObject()) {
				throw new IllegalArgumentException("Model metrics JSON is invalid");
			}
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Model metrics JSON is invalid", exception);
		}
	}

	private void requireDataset(UUID datasetId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM ml_training_datasets WHERE id = ?", Integer.class, datasetId);
		if (count == null || count != 1) {
			throw new IllegalArgumentException("Unknown training dataset: " + datasetId);
		}
	}
}
