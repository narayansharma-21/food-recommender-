package com.narayansharma.foodrecommender.menu.source;

import java.util.UUID;

public record OfficialMenuSubmissionResponse(
		UUID menuId,
		UUID sourceId,
		String status) {
}
