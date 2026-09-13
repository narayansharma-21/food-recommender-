package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import java.util.List;

interface PdfOcrProcessor {
	List<OcrPage> extract(byte[] pdf);
}
