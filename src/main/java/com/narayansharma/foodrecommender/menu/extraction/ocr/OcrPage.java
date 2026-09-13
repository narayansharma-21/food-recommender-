package com.narayansharma.foodrecommender.menu.extraction.ocr;

public record OcrPage(int pageNumber, String text, double confidence) {
	public OcrPage {
		if (pageNumber < 1) {
			throw new IllegalArgumentException("OCR page number must be positive");
		}
		if (text == null) {
			throw new IllegalArgumentException("OCR page text is required");
		}
		if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
			throw new IllegalArgumentException("OCR confidence must be between 0 and 1");
		}
	}
}
