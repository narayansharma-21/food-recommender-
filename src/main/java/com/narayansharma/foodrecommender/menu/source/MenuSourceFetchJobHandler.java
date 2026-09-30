package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.platform.jobs.BackgroundJobHandler;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class MenuSourceFetchJobHandler implements BackgroundJobHandler {
	static final String JOB_TYPE = "MENU_SOURCE_FETCH";

	private final ObjectMapper objectMapper;
	private final OfficialMenuFetchService fetchService;

	MenuSourceFetchJobHandler(ObjectMapper objectMapper, OfficialMenuFetchService fetchService) {
		this.objectMapper = objectMapper;
		this.fetchService = fetchService;
	}

	@Override
	public String jobType() {
		return JOB_TYPE;
	}

	@Override
	public void handle(String payload) {
		try {
			MenuSourceFetchJobPayload job = objectMapper.readValue(payload, MenuSourceFetchJobPayload.class);
			if (job.sourceId() == null) {
				throw new IllegalArgumentException("Menu source fetch job requires a source ID");
			}
			fetchService.fetch(job.sourceId());
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Menu source fetch job payload is invalid", exception);
		}
	}
}
