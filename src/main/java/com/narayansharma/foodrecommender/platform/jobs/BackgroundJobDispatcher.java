package com.narayansharma.foodrecommender.platform.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class BackgroundJobDispatcher {
	private final Map<String, BackgroundJobHandler> handlers;

	BackgroundJobDispatcher(List<BackgroundJobHandler> handlers) {
		Map<String, BackgroundJobHandler> registeredHandlers = new HashMap<>();
		for (BackgroundJobHandler handler : handlers) {
			BackgroundJobHandler duplicate = registeredHandlers.put(handler.jobType(), handler);
			if (duplicate != null) {
				throw new IllegalStateException("Duplicate background job handler: " + handler.jobType());
			}
		}
		this.handlers = Map.copyOf(registeredHandlers);
	}

	void dispatch(ClaimedJob job) {
		BackgroundJobHandler handler = handlers.get(job.jobType());
		if (handler == null) {
			throw new IllegalStateException("No handler for background job type: " + job.jobType());
		}
		handler.handle(job.payload());
	}
}
