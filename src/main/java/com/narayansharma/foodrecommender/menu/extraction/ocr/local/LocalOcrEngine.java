package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrEngine;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalOcrEngine implements OcrEngine {
	private final TesseractRunner runner;
	private final PdfOcrProcessor pdfProcessor;
	private final HtmlMenuTextExtractor htmlTextExtractor;
	private final String providerVersion;

	LocalOcrEngine(
			TesseractRunner runner,
			PdfOcrProcessor pdfProcessor,
			HtmlMenuTextExtractor htmlTextExtractor,
			@Value("${ocr.local.provider-version:local-ocr-v1}") String providerVersion) {
		if (providerVersion == null || providerVersion.isBlank() || providerVersion.length() > 100) {
			throw new IllegalArgumentException("Local OCR provider version is invalid");
		}
		this.runner = runner;
		this.pdfProcessor = pdfProcessor;
		this.htmlTextExtractor = htmlTextExtractor;
		this.providerVersion = providerVersion;
	}

	@Override
	public OcrResult extract(OcrDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("OCR document is required");
		}
		if (document.mediaType().equals("application/pdf")) {
			return new OcrResult("local_ocr", providerVersion, pdfProcessor.extract(document.content()));
		}
		if (document.mediaType().equals("text/html")
				|| document.mediaType().equals("application/xhtml+xml")) {
			return new OcrResult(
					"local_ocr",
					providerVersion,
					List.of(new OcrPage(1, htmlTextExtractor.extract(document.content()), 1)));
		}
		if (!document.mediaType().equals("image/jpeg") && !document.mediaType().equals("image/png")) {
			throw new IllegalArgumentException("OCR document media type is unsupported");
		}
		TesseractOutput output = runner.extract(document.content());
		return new OcrResult(
				"local_ocr",
				providerVersion,
				List.of(new OcrPage(1, output.text(), output.confidence())));
	}
}
