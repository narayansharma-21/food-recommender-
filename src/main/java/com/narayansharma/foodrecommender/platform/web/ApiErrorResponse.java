package com.narayansharma.foodrecommender.platform.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldViolation> fieldViolations) {

	public record FieldViolation(String field, String message) {
	}
}
