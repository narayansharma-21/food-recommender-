package com.narayansharma.foodrecommender.menu.extraction.ocr;

import java.util.List;

public record OcrResult(
		String provider,
		String providerVersion,
		List<OcrPage> pages) {
	public OcrResult {
		if (provider == null || !provider.matches("[a-z][a-z0-9_-]{0,49}")) {
			throw new IllegalArgumentException("OCR provider is invalid");
		}
		if (providerVersion == null || providerVersion.isBlank() || providerVersion.length() > 100) {
			throw new IllegalArgumentException("OCR provider version is required");
		}
		if (pages == null || pages.isEmpty()) {
			throw new IllegalArgumentException("OCR result requires at least one page");
		}
		for (int index = 0; index < pages.size(); index++) {
			if (pages.get(index) == null || pages.get(index).pageNumber() != index + 1) {
				throw new IllegalArgumentException("OCR pages must be ordered from page one");
			}
		}
		pages = List.copyOf(pages);
	}

	public String fullText() {
		return String.join("\n\f\n", pages.stream().map(OcrPage::text).toList());
	}
}
