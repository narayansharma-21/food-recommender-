package com.narayansharma.foodrecommender.catalog.query;

import java.util.List;
import java.util.UUID;

public record MenuSectionView(
		UUID id,
		String displayName,
		List<MenuItemView> items) {
	public MenuSectionView {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
