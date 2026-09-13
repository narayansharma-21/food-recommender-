package com.narayansharma.foodrecommender.catalog.dish.extraction;

import java.util.List;

public interface DishAttributeExtractor {
	List<DishAttributeCandidate> extract(MenuItemText item);
}
