package com.narayansharma.foodrecommender.menu.extraction.ocr;

import java.util.Set;

public record OcrDocument(String mediaType, byte[] content) {
	private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
			"image/jpeg", "image/png", "application/pdf");

	public OcrDocument {
		if (mediaType == null || !SUPPORTED_MEDIA_TYPES.contains(mediaType)) {
			throw new IllegalArgumentException("OCR document type is unsupported");
		}
		if (content == null || content.length == 0) {
			throw new IllegalArgumentException("OCR document content is required");
		}
		content = content.clone();
	}

	@Override
	public byte[] content() {
		return content.clone();
	}
}
