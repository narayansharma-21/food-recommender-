package com.narayansharma.foodrecommender.menu.extraction.evidence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import java.util.List;
import org.junit.jupiter.api.Test;

class FieldEvidenceContractTest {
	@Test
	void requiresOcrPageNumbersAndUniqueFieldPaths() {
		assertThatThrownBy(() -> new FieldProvenance(ProvenanceSource.OCR_TEXT, null, "Menu line"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("page number");

		FieldEvidence field = new FieldEvidence(
				"sections[0].name",
				0.8,
				List.of(new FieldProvenance(ProvenanceSource.OCR_TEXT, 1, "ENTREES")));
		assertThatThrownBy(() -> new AttributedExtractedMenu(
				new ExtractedMenu(List.of()),
				List.of(field, field)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unique");
	}
}
