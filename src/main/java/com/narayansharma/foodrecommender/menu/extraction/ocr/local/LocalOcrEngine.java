package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrEngine;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrException;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalOcrEngine implements OcrEngine {
	private final TesseractRunner runner;
	private final String providerVersion;

	LocalOcrEngine(
			TesseractRunner runner,
			@Value("${ocr.local.provider-version:local-ocr-v1}") String providerVersion) {
		if (providerVersion == null || providerVersion.isBlank() || providerVersion.length() > 100) {
			throw new IllegalArgumentException("Local OCR provider version is invalid");
		}
		this.runner = runner;
		this.providerVersion = providerVersion;
	}

	@Override
	public OcrResult extract(OcrDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("OCR document is required");
		}
		if (!document.mediaType().startsWith("image/")) {
			throw new OcrException("Local OCR cannot yet process this document type");
		}
		TesseractOutput output = runner.extract(document.content());
		return new OcrResult(
				"local_ocr",
				providerVersion,
				List.of(new OcrPage(1, output.text(), output.confidence())));
	}
}
