package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

interface TesseractRunner {
	TesseractOutput extract(byte[] image);
}
