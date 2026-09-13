package com.narayansharma.foodrecommender.menu.extraction.structured;

import com.narayansharma.foodrecommender.menu.extraction.evidence.AttributedExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;

public interface MenuTextExtractor {
	AttributedExtractedMenu extractWithEvidence(OcrResult ocrResult);

	default ExtractedMenu extract(OcrResult ocrResult) {
		return extractWithEvidence(ocrResult).menu();
	}
}
