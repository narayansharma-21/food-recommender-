package com.narayansharma.foodrecommender.ml.registry;

import java.math.BigDecimal;
import java.util.UUID;

public record ModelRegistration(
		String modelVersion,
		String modelType,
		UUID datasetId,
		String artifactObjectKey,
		String artifactSha256,
		BigDecimal overallMae,
		BigDecimal baselineMae,
		String metricsJson,
		String codeVersion) {
}
