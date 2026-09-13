package com.narayansharma.foodrecommender.identity.consent;

import java.time.Instant;
import java.util.UUID;

public record UserConsent(
		UUID id,
		ConsentType type,
		int sequence,
		boolean granted,
		String policyVersion,
		Instant recordedAt) {
}
