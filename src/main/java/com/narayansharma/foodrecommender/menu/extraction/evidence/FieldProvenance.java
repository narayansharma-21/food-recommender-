package com.narayansharma.foodrecommender.menu.extraction.evidence;

public record FieldProvenance(
		ProvenanceSource source,
		Integer pageNumber,
		String sourceText) {
	public FieldProvenance {
		if (source == null || sourceText == null || sourceText.isBlank() || sourceText.length() > 500) {
			throw new IllegalArgumentException("Field provenance is invalid");
		}
		if (source == ProvenanceSource.OCR_TEXT && (pageNumber == null || pageNumber < 1)) {
			throw new IllegalArgumentException("OCR provenance requires a page number");
		}
		if (source == ProvenanceSource.INFERRED && pageNumber != null) {
			throw new IllegalArgumentException("Inferred provenance cannot claim an OCR page");
		}
	}
}
