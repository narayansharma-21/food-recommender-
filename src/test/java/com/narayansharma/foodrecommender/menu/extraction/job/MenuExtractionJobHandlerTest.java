package com.narayansharma.foodrecommender.menu.extraction.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.menu.extraction.evidence.AttributedExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrPage;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.persistence.StoredOriginalExtraction;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenu;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuItem;
import com.narayansharma.foodrecommender.menu.extraction.structured.ExtractedMenuSection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MenuExtractionJobHandlerTest {
	@Test
	void runsTheCompleteExtractionPipelineForOneMenuVersion() throws Exception {
		UUID menuVersionId = UUID.randomUUID();
		OcrDocument document = new OcrDocument("image/png", new byte[] {1});
		OcrResult ocrResult = new OcrResult("test", "1", List.of(new OcrPage(1, "Burger $12", 0.9)));
		ExtractedMenu menu = new ExtractedMenu(List.of(new ExtractedMenuSection(
				"Menu",
				List.of(new ExtractedMenuItem("Burger", null, new BigDecimal("12"), "USD", List.of())))));
		AttributedExtractedMenu extraction = new AttributedExtractedMenu(menu, List.of());
		AtomicReference<UUID> storedVersion = new AtomicReference<>();

		MenuExtractionJobHandler handler = new MenuExtractionJobHandler(
				new ObjectMapper(),
				requestedVersion -> {
					assertThat(requestedVersion).isEqualTo(menuVersionId);
					return document;
				},
				requestedDocument -> {
					assertThat(requestedDocument).isEqualTo(document);
					return ocrResult;
				},
				requestedOcr -> {
					assertThat(requestedOcr).isEqualTo(ocrResult);
					return extraction;
				},
				(version, ocr, result, parserVersion) -> {
					storedVersion.set(version);
					assertThat(ocr).isEqualTo(ocrResult);
					assertThat(result).isEqualTo(extraction);
					assertThat(parserVersion).isEqualTo("rules-v1");
					return new StoredOriginalExtraction(version, version, Instant.EPOCH, true);
				},
				"rules-v1");

		handler.handle(new ObjectMapper().writeValueAsString(new MenuExtractionJobPayload(menuVersionId)));

		assertThat(handler.jobType()).isEqualTo("MENU_EXTRACTION");
		assertThat(storedVersion).hasValue(menuVersionId);
	}

	@Test
	void rejectsMalformedPayloadBeforeLoadingADocument() {
		MenuExtractionJobHandler handler = new MenuExtractionJobHandler(
				new ObjectMapper(),
				version -> { throw new AssertionError("Document should not load"); },
				document -> { throw new AssertionError("OCR should not run"); },
				ocr -> { throw new AssertionError("Parser should not run"); },
				(version, ocr, extraction, parserVersion) -> { throw new AssertionError("Result should not store"); },
				"rules-v1");

		assertThatThrownBy(() -> handler.handle("not-json"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("payload is invalid");
	}
}
