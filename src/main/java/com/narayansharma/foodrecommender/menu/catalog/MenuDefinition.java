package com.narayansharma.foodrecommender.menu.catalog;

import java.util.UUID;

public record MenuDefinition(
		UUID menuId,
		UUID locationId,
		String menuKey,
		String displayName,
		boolean created) {
}
