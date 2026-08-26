package com.narayansharma.foodrecommender.catalog.changes;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record RestaurantChangeRequest(
		UUID id,
		UUID locationId,
		String requesterReference,
		RestaurantChangeRequestKind kind,
		RestaurantCorrectionField correctionField,
		String proposedValue,
		String reason,
		URI evidenceUrl,
		RestaurantChangeRequestStatus status,
		String reviewerReference,
		String resolutionNote,
		Instant createdAt,
		Instant reviewedAt) {
}
