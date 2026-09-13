package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class TesseractTsvParser {
	TesseractOutput parse(String tsv) {
		if (tsv == null) {
			throw new IllegalArgumentException("Tesseract output is required");
		}
		StringBuilder text = new StringBuilder();
		String previousLine = null;
		List<Double> confidences = new ArrayList<>();
		for (String row : tsv.split("\\R")) {
			String[] columns = row.split("\\t", 12);
			if (columns.length < 12 || !"5".equals(columns[0]) || columns[11].isBlank()) {
				continue;
			}
			String line = String.join(":", columns[1], columns[2], columns[3], columns[4]);
			if (!line.equals(previousLine) && !text.isEmpty()) {
				text.append('\n');
			} else if (line.equals(previousLine)) {
				text.append(' ');
			}
			text.append(columns[11].trim());
			previousLine = line;
			try {
				double confidence = Double.parseDouble(columns[10]);
				if (Double.isFinite(confidence) && confidence >= 0) {
					confidences.add(Math.min(confidence, 100));
				}
			} catch (NumberFormatException ignored) {
				// Keep valid recognized text even when one confidence value is malformed.
			}
		}
		double average = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0) / 100;
		return new TesseractOutput(text.toString(), average);
	}
}
