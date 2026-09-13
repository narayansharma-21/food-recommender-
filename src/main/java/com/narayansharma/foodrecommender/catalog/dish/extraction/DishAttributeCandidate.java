package com.narayansharma.foodrecommender.catalog.dish.extraction;

public record DishAttributeCandidate(
		DishAttributeType type,
		String attributeKey,
		AttributeEvidenceType evidenceType,
		double confidence,
		String matchedText) {
	public DishAttributeCandidate {
		if (type == null
				|| evidenceType == null
				|| evidenceType == AttributeEvidenceType.USER_CORRECTED
				|| attributeKey == null
				|| !attributeKey.matches("[a-z][a-z0-9_]{0,99}")
				|| !Double.isFinite(confidence)
				|| confidence < 0
				|| confidence > 1
				|| matchedText == null
				|| matchedText.isBlank()
				|| matchedText.length() > 100) {
			throw new IllegalArgumentException("Dish attribute candidate is invalid");
		}
	}
}
