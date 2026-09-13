package com.narayansharma.foodrecommender.menu.extraction.job;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import java.util.UUID;

interface MenuVersionDocumentLoader {
	OcrDocument load(UUID menuVersionId);
}
