package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrException;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PdfBoxOcrProcessor implements PdfOcrProcessor {
	private final TesseractRunner runner;
	private final int maxBytes;
	private final int maxPages;
	private final int renderDpi;
	private final long maxRenderedPixels;
	private final int minimumEmbeddedTextCharacters;

	PdfBoxOcrProcessor(
			TesseractRunner runner,
			@Value("${ocr.pdf.max-bytes:10485760}") int maxBytes,
			@Value("${ocr.pdf.max-pages:20}") int maxPages,
			@Value("${ocr.pdf.render-dpi:200}") int renderDpi,
			@Value("${ocr.pdf.max-rendered-pixels:25000000}") long maxRenderedPixels,
			@Value("${ocr.pdf.minimum-embedded-text-characters:12}") int minimumEmbeddedTextCharacters) {
		if (maxBytes < 1 || maxBytes > 50 * 1024 * 1024) {
			throw new IllegalArgumentException("PDF byte limit is invalid");
		}
		if (maxPages < 1 || maxPages > 100) {
			throw new IllegalArgumentException("PDF page limit is invalid");
		}
		if (renderDpi < 72 || renderDpi > 300) {
			throw new IllegalArgumentException("PDF render DPI is invalid");
		}
		if (maxRenderedPixels < 1_000_000 || maxRenderedPixels > 50_000_000) {
			throw new IllegalArgumentException("PDF rendered-pixel limit is invalid");
		}
		if (minimumEmbeddedTextCharacters < 1 || minimumEmbeddedTextCharacters > 100) {
			throw new IllegalArgumentException("PDF embedded-text threshold is invalid");
		}
		this.runner = runner;
		this.maxBytes = maxBytes;
		this.maxPages = maxPages;
		this.renderDpi = renderDpi;
		this.maxRenderedPixels = maxRenderedPixels;
		this.minimumEmbeddedTextCharacters = minimumEmbeddedTextCharacters;
	}

	@Override
	public List<OcrPage> extract(byte[] pdf) {
		if (pdf.length > maxBytes) {
			throw new OcrException("PDF exceeds the local OCR size limit");
		}
		try (PDDocument document = Loader.loadPDF(pdf)) {
			int pageCount = document.getNumberOfPages();
			if (pageCount < 1) {
				throw new OcrException("PDF has no pages");
			}
			if (pageCount > maxPages) {
				throw new OcrException("PDF exceeds the local OCR page limit");
			}
			PDFRenderer renderer = new PDFRenderer(document);
			List<OcrPage> pages = new ArrayList<>(pageCount);
			for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
				pages.add(extractPage(document, renderer, pageIndex));
			}
			return pages;
		} catch (InvalidPasswordException exception) {
			throw new OcrException("Encrypted PDFs are unsupported", exception);
		} catch (IOException exception) {
			throw new OcrException("PDF could not be read", exception);
		}
	}

	private OcrPage extractPage(PDDocument document, PDFRenderer renderer, int pageIndex) throws IOException {
		String embeddedText = extractEmbeddedText(document, pageIndex).strip();
		if (embeddedText.length() >= minimumEmbeddedTextCharacters) {
			return new OcrPage(pageIndex + 1, embeddedText, 1);
		}
		var page = document.getPage(pageIndex);
		validateRenderedSize(page.getCropBox(), page.getUserUnit());
		var image = renderer.renderImageWithDPI(pageIndex, renderDpi, ImageType.RGB);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			if (!ImageIO.write(image, "png", output)) {
				throw new IOException("PNG encoder is unavailable");
			}
			TesseractOutput result = runner.extract(output.toByteArray());
			return new OcrPage(pageIndex + 1, result.text(), result.confidence());
		}
	}

	private String extractEmbeddedText(PDDocument document, int pageIndex) throws IOException {
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setStartPage(pageIndex + 1);
		stripper.setEndPage(pageIndex + 1);
		return stripper.getText(document);
	}

	private void validateRenderedSize(PDRectangle pageBounds, float userUnit) {
		long width = (long) Math.ceil(pageBounds.getWidth() * userUnit * renderDpi / 72.0);
		long height = (long) Math.ceil(pageBounds.getHeight() * userUnit * renderDpi / 72.0);
		if (width < 1 || height < 1 || width > maxRenderedPixels / height) {
			throw new OcrException("PDF page is too large to OCR safely");
		}
	}
}
