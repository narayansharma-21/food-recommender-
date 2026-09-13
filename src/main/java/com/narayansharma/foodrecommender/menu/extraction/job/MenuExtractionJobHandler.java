package com.narayansharma.foodrecommender.menu.extraction.job;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrEngine;
import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrResult;
import com.narayansharma.foodrecommender.menu.extraction.persistence.MenuExtractionResultStore;
import com.narayansharma.foodrecommender.menu.extraction.structured.MenuTextExtractor;
import com.narayansharma.foodrecommender.platform.jobs.BackgroundJobHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class MenuExtractionJobHandler implements BackgroundJobHandler {
	static final String JOB_TYPE = "MENU_EXTRACTION";

	private final ObjectMapper objectMapper;
	private final MenuVersionDocumentLoader documentLoader;
	private final OcrEngine ocrEngine;
	private final MenuTextExtractor textExtractor;
	private final MenuExtractionResultStore resultStore;
	private final String parserVersion;

	MenuExtractionJobHandler(
			ObjectMapper objectMapper,
			MenuVersionDocumentLoader documentLoader,
			OcrEngine ocrEngine,
			MenuTextExtractor textExtractor,
			MenuExtractionResultStore resultStore,
			@Value("${menu.extraction.parser-version:rules-v1}") String parserVersion) {
		if (parserVersion == null || parserVersion.isBlank() || parserVersion.length() > 100) {
			throw new IllegalArgumentException("Menu parser version is invalid");
		}
		this.objectMapper = objectMapper;
		this.documentLoader = documentLoader;
		this.ocrEngine = ocrEngine;
		this.textExtractor = textExtractor;
		this.resultStore = resultStore;
		this.parserVersion = parserVersion;
	}

	@Override
	public String jobType() {
		return JOB_TYPE;
	}

	@Override
	public void handle(String payload) {
		MenuExtractionJobPayload job = parsePayload(payload);
		if (job.menuVersionId() == null) {
			throw new IllegalArgumentException("Menu extraction job requires a menu version");
		}
		OcrResult ocrResult = ocrEngine.extract(documentLoader.load(job.menuVersionId()));
		var extraction = textExtractor.extractWithEvidence(ocrResult);
		resultStore.store(job.menuVersionId(), ocrResult, extraction, parserVersion);
	}

	private MenuExtractionJobPayload parsePayload(String payload) {
		try {
			MenuExtractionJobPayload job = objectMapper.readValue(payload, MenuExtractionJobPayload.class);
			if (job == null) {
				throw new IllegalArgumentException("Menu extraction job payload is empty");
			}
			return job;
		} catch (JacksonException | IllegalArgumentException exception) {
			throw new IllegalArgumentException("Menu extraction job payload is invalid", exception);
		}
	}
}
