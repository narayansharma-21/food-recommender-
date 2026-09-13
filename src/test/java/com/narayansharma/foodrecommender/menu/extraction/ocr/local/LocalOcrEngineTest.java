package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalOcrEngineTest {
	@Test
	void extractsImageTextThroughTheProviderContract() {
		LocalOcrEngine engine = new LocalOcrEngine(
				image -> new TesseractOutput("Margherita Pizza  14", 0.92),
				pdf -> { throw new AssertionError("PDF processor should not be called"); },
				"test-v1");

		OcrResult result = engine.extract(new OcrDocument("image/png", new byte[] {1, 2, 3}));

		assertThat(result.provider()).isEqualTo("local_ocr");
		assertThat(result.pages()).singleElement().satisfies(page -> {
			assertThat(page.text()).isEqualTo("Margherita Pizza  14");
			assertThat(page.confidence()).isEqualTo(0.92);
		});
	}

	@Test
	void extractsPdfPagesThroughTheProviderContract() {
		LocalOcrEngine engine = new LocalOcrEngine(
				image -> { throw new AssertionError("Image runner should not be called"); },
				pdf -> List.of(new OcrPage(1, "Menu", 1)),
				"test-v1");

		OcrResult result = engine.extract(new OcrDocument("application/pdf", new byte[] {1}));

		assertThat(result.fullText()).isEqualTo("Menu");
	}
}
