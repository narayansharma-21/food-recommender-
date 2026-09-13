package com.narayansharma.foodrecommender.menu.extraction.persistence;

import com.narayansharma.foodrecommender.menu.extraction.evidence.AttributedExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import java.util.UUID;

public interface MenuExtractionResultStore {
	StoredOriginalExtraction store(
			UUID menuVersionId,
			OcrResult ocrResult,
			AttributedExtractedMenu extraction,
			String parserVersion);
}
