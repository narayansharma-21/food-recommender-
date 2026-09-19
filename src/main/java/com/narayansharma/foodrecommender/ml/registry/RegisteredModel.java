package com.narayansharma.foodrecommender.ml.registry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegisteredModel(
		UUID id,
		String modelVersion,
		String modelType,
		UUID datasetId,
		String artifactObjectKey,
		String artifactSha256,
		BigDecimal overallMae,
		BigDecimal baselineMae,
		String codeVersion,
		String status,
		Instant createdAt,
		Instant promotedAt,
		Instant retiredAt) {
}
