package com.narayansharma.foodrecommender.feedback;

import java.math.BigDecimal;

public record ExtractedTraitSignal(
		String traitKey,
		String sentiment,
		BigDecimal confidence,
		String evidenceText,
		String extractorVersion) {
}
