package com.narayansharma.foodrecommender.feedback;

import java.util.UUID;

public record ReportRatingResponse(UUID caseId, String status) {
}
