package com.narayansharma.foodrecommender.menu.extraction.validation;

import java.util.List;

public class MenuSchemaValidationException extends RuntimeException {
	private final List<MenuSchemaViolation> violations;

	MenuSchemaValidationException(List<MenuSchemaViolation> violations) {
		super("Extracted menu failed schema validation with " + violations.size() + " violation(s)");
		this.violations = List.copyOf(violations);
	}

	public List<MenuSchemaViolation> violations() {
		return violations;
	}
}
