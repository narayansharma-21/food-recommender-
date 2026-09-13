package com.narayansharma.foodrecommender.menu.extraction.validation;

public record MenuSchemaViolation(String path, String message) {
	public MenuSchemaViolation {
		if (path == null || path.isBlank() || message == null || message.isBlank()) {
			throw new IllegalArgumentException("Menu schema violation fields are required");
		}
	}
}
