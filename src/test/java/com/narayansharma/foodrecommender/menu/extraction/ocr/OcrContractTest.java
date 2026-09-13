package com.narayansharma.foodrecommender.menu.extraction.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrContractTest {
	@Test
	void protectsDocumentBytesFromMutation() {
		byte[] original = {1, 2, 3};
		OcrDocument document = new OcrDocument("image/png", original);

		original[0] = 9;
		byte[] returned = document.content();
		returned[1] = 9;

		assertThat(document.content()).containsExactly(1, 2, 3);
	}

	@Test
	void keepsPagesImmutableAndBuildsFullText() {
		List<OcrPage> pages = new ArrayList<>();
		pages.add(new OcrPage(1, "Starters", 0.91));
		OcrResult result = new OcrResult("local_ocr", "1.0", pages);

		pages.clear();

		assertThat(result.pages()).hasSize(1);
		assertThat(result.fullText()).isEqualTo("Starters");
		assertThatThrownBy(() -> result.pages().add(new OcrPage(2, "Entrees", 0.90)))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void rejectsUnsupportedDocumentsAndInvalidConfidence() {
		assertThatThrownBy(() -> new OcrDocument("text/plain", new byte[] {1}))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unsupported");
		assertThatThrownBy(() -> new OcrPage(1, "Menu", Double.NaN))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("confidence");
	}
}
