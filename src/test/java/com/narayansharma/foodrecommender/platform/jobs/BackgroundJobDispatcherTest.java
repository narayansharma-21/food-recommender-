package com.narayansharma.foodrecommender.platform.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BackgroundJobDispatcherTest {
	@Test
	void dispatchesToMatchingHandler() {
		AtomicReference<String> receivedPayload = new AtomicReference<>();
		BackgroundJobHandler handler = handler("MENU_EXTRACT", receivedPayload);
		BackgroundJobDispatcher dispatcher = new BackgroundJobDispatcher(List.of(handler));

		dispatcher.dispatch(new ClaimedJob(UUID.randomUUID(), "MENU_EXTRACT", "payload", 1));

		assertThat(receivedPayload).hasValue("payload");
	}

	@Test
	void rejectsDuplicateHandlerTypes() {
		BackgroundJobHandler first = handler("MENU_EXTRACT", new AtomicReference<>());
		BackgroundJobHandler second = handler("MENU_EXTRACT", new AtomicReference<>());

		assertThatThrownBy(() -> new BackgroundJobDispatcher(List.of(first, second)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Duplicate");
	}

	private BackgroundJobHandler handler(String type, AtomicReference<String> receivedPayload) {
		return new BackgroundJobHandler() {
			@Override
			public String jobType() {
				return type;
			}

			@Override
			public void handle(String payload) {
				receivedPayload.set(payload);
			}
		};
	}
}
