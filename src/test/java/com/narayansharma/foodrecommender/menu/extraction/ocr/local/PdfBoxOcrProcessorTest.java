package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfBoxOcrProcessorTest {
	@Test
	void usesEmbeddedTextWithoutRunningImageOcr() throws IOException {
		PdfBoxOcrProcessor processor = processor(image -> {
			throw new AssertionError("Image OCR should not run for a text PDF");
		}, 20);

		var pages = processor.extract(textPdf("Margherita Pizza $14"));

		assertThat(pages).singleElement().satisfies(page -> {
			assertThat(page.text()).contains("Margherita Pizza $14");
			assertThat(page.confidence()).isEqualTo(1);
		});
	}

	@Test
	void rendersPagesWithoutEnoughEmbeddedTextForOcr() throws IOException {
		AtomicInteger imageSize = new AtomicInteger();
		PdfBoxOcrProcessor processor = processor(image -> {
			imageSize.set(image.length);
			return new TesseractOutput("Scanned Menu", 0.81);
		}, 20);

		var pages = processor.extract(blankPdf(1));

		assertThat(imageSize).hasPositiveValue();
		assertThat(pages).singleElement().satisfies(page -> {
			assertThat(page.text()).isEqualTo("Scanned Menu");
			assertThat(page.confidence()).isEqualTo(0.81);
		});
	}

	@Test
	void rejectsDocumentsOverThePageLimit() throws IOException {
		PdfBoxOcrProcessor processor = processor(image -> new TesseractOutput("unused", 1), 1);

		assertThatThrownBy(() -> processor.extract(blankPdf(2)))
				.isInstanceOf(OcrException.class)
				.hasMessageContaining("page limit");
	}

	@Test
	void rejectsPagesThatWouldRenderTooManyPixels() throws IOException {
		PdfBoxOcrProcessor processor = processor(image -> {
			throw new AssertionError("Oversized page should not reach image OCR");
		}, 20);

		assertThatThrownBy(() -> processor.extract(blankPdf(new PDRectangle(10_000, 10_000))))
				.isInstanceOf(OcrException.class)
				.hasMessageContaining("too large");
	}

	private PdfBoxOcrProcessor processor(TesseractRunner runner, int maxPages) {
		return new PdfBoxOcrProcessor(runner, 10 * 1024 * 1024, maxPages, 72, 25_000_000, 12);
	}

	private byte[] textPdf(String text) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(72, 720);
				content.showText(text);
				content.endText();
			}
			return save(document);
		}
	}

	private byte[] blankPdf(int pageCount) throws IOException {
		try (PDDocument document = new PDDocument()) {
			for (int page = 0; page < pageCount; page++) {
				document.addPage(new PDPage());
			}
			return save(document);
		}
	}

	private byte[] blankPdf(PDRectangle pageSize) throws IOException {
		try (PDDocument document = new PDDocument()) {
			document.addPage(new PDPage(pageSize));
			return save(document);
		}
	}

	private byte[] save(PDDocument document) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		document.save(output);
		return output.toByteArray();
	}
}
