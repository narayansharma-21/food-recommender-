package com.narayansharma.foodrecommender.ml.registry;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelPromotionService {
	private final JdbcTemplate jdbcTemplate;
	private final ModelRegistryService registry;
	private final Clock clock;

	public ModelPromotionService(JdbcTemplate jdbcTemplate, ModelRegistryService registry, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.registry = registry;
		this.clock = clock;
	}

	@Transactional
	public RegisteredModel promote(UUID modelId, String promotedBy, String reason) {
		validate(modelId, promotedBy, reason);
		List<UUID> lockedModels = jdbcTemplate.query(
				"SELECT id FROM ml_model_versions ORDER BY id FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class));
		if (!lockedModels.contains(modelId)) {
			throw new IllegalArgumentException("Unknown model: " + modelId);
		}
		RegisteredModel target = registry.get(modelId);
		if ("ACTIVE".equals(target.status())) {
			throw new IllegalArgumentException("Model is already active");
		}
		if (target.overallMae().compareTo(target.baselineMae()) >= 0) {
			throw new IllegalArgumentException("Model must beat its baseline before promotion");
		}
		List<UUID> activeModels = jdbcTemplate.query(
				"SELECT id FROM ml_model_versions WHERE status = 'ACTIVE'",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class));
		if (activeModels.size() > 1) {
			throw new IllegalStateException("More than one ML model is active");
		}
		UUID previousModelId = activeModels.isEmpty() ? null : activeModels.getFirst();
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		if (previousModelId != null) {
			jdbcTemplate.update("""
					UPDATE ml_model_versions
					SET status = 'RETIRED', retired_at = ?
					WHERE id = ?
					""", Timestamp.from(now), previousModelId);
		}
		jdbcTemplate.update("""
				UPDATE ml_model_versions
				SET status = 'ACTIVE', promoted_at = ?, retired_at = NULL
				WHERE id = ?
				""", Timestamp.from(now), modelId);
		jdbcTemplate.update("""
				INSERT INTO ml_model_promotions (
				    id, from_model_id, to_model_id, promoted_by, reason, created_at
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				previousModelId,
				modelId,
				promotedBy.strip(),
				reason.strip(),
				Timestamp.from(now));
		return registry.get(modelId);
	}

	private void validate(UUID modelId, String promotedBy, String reason) {
		if (modelId == null) {
			throw new IllegalArgumentException("Model ID is required");
		}
		if (promotedBy == null || promotedBy.isBlank() || promotedBy.strip().length() > 200) {
			throw new IllegalArgumentException("Model promotion actor is invalid");
		}
		if (reason == null || reason.isBlank() || reason.strip().length() > 1000) {
			throw new IllegalArgumentException("Model promotion reason is invalid");
		}
	}
}
