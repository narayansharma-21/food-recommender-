package com.narayansharma.foodrecommender.menu.extraction.job;

import com.narayansharma.foodrecommender.platform.jobs.BackgroundJobService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class MenuExtractionJobQueue {
	private final BackgroundJobService jobService;
	private final ObjectMapper objectMapper;

	public MenuExtractionJobQueue(BackgroundJobService jobService, ObjectMapper objectMapper) {
		this.jobService = jobService;
		this.objectMapper = objectMapper;
	}

	public UUID enqueue(UUID menuVersionId) {
		if (menuVersionId == null) {
			throw new IllegalArgumentException("Menu version is required");
		}
		try {
			String payload = objectMapper.writeValueAsString(new MenuExtractionJobPayload(menuVersionId));
			return jobService.enqueue(MenuExtractionJobHandler.JOB_TYPE, payload);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Menu extraction job could not be serialized", exception);
		}
	}
}
