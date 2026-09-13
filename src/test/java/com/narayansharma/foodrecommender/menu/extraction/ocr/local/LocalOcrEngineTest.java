package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrException;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import org.junit.jupiter.api.Test;

class LocalOcrEngineTest {
	@Test
	void extractsImageTextThroughTheProviderContract() {
		LocalOcrEngine engine = new LocalOcrEngine(
				image -> new TesseractOutput("Margherita Pizza  14", 0.92),
				"test-v1");

		OcrResult result = engine.extract(new OcrDocument("image/png", new byte[] {1, 2, 3}));

		assertThat(result.provider()).isEqualTo("local_ocr");
		assertThat(result.pages()).singleElement().satisfies(page -> {
			assertThat(page.text()).isEqualTo("Margherita Pizza  14");
			assertThat(page.confidence()).isEqualTo(0.92);
		});
	}

	@Test
	void rejectsPdfUntilPdfSupportIsAdded() {
		LocalOcrEngine engine = new LocalOcrEngine(
				image -> new TesseractOutput("unused", 1),
				"test-v1");

		assertThatThrownBy(() -> engine.extract(new OcrDocument("application/pdf", new byte[] {1})))
				.isInstanceOf(OcrException.class)
				.hasMessageContaining("cannot yet process");
	}
}
