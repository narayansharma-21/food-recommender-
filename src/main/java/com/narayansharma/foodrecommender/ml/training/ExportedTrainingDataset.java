package com.narayansharma.foodrecommender.ml.training;

import java.time.Instant;
import java.util.UUID;

public record ExportedTrainingDataset(
		UUID id,
		String schemaVersion,
		Instant fromInclusive,
		Instant cutoffExclusive,
		String objectKey,
		String sha256,
		int rowCount,
		String codeVersion,
		Instant createdAt) {
}
