package com.narayansharma.foodrecommender.menu.extraction.evidence;

import java.util.List;

public record FieldEvidence(String path, double confidence, List<FieldProvenance> provenance) {
	public FieldEvidence {
		if (path == null || path.isBlank() || !Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
			throw new IllegalArgumentException("Field evidence is invalid");
		}
		if (provenance == null || provenance.isEmpty()) {
			throw new IllegalArgumentException("Field evidence requires provenance");
		}
		provenance = List.copyOf(provenance);
	}
}
