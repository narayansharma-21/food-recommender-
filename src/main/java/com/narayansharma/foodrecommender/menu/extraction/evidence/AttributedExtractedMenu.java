package com.narayansharma.foodrecommender.menu.extraction.evidence;

import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record AttributedExtractedMenu(ExtractedMenu menu, List<FieldEvidence> fields) {
	public AttributedExtractedMenu {
		if (menu == null || fields == null) {
			throw new IllegalArgumentException("Attributed menu fields are required");
		}
		Set<String> paths = new HashSet<>();
		for (FieldEvidence field : fields) {
			if (field == null) {
				throw new IllegalArgumentException("Attributed menu fields cannot contain null");
			}
			if (!paths.add(field.path())) {
				throw new IllegalArgumentException("Attributed menu field paths must be unique");
			}
		}
		fields = List.copyOf(fields);
	}
}
