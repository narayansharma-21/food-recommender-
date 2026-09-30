package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JavaMenuHttpTransportTest {
	@Test
	void requiresAPositiveConnectTimeout() {
		assertThatThrownBy(() -> new JavaMenuHttpTransport(Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("positive");
	}
}
