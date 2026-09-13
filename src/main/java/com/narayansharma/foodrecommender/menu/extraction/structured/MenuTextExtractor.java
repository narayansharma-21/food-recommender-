package com.narayansharma.foodrecommender.menu.extraction.structured;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;

public interface MenuTextExtractor {
	ExtractedMenu extract(OcrResult ocrResult);
}
