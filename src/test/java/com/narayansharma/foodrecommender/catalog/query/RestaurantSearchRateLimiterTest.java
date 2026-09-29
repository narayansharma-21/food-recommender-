package com.narayansharma.foodrecommender.catalog.query;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestaurantSearchRateLimiterTest {
	@Test
	void limitsEachUserAndResetsAfterTheWindow() {
		MutableClock clock = new MutableClock();
		RestaurantSearchRateLimiter limiter = new RestaurantSearchRateLimiter(clock, 2, Duration.ofMinutes(1));
		UUID userId = UUID.randomUUID();

		limiter.requireAllowed(userId);
		limiter.requireAllowed(userId);
		assertThatThrownBy(() -> limiter.requireAllowed(userId))
				.isInstanceOf(ApiException.class)
				.hasMessage("Too many restaurant searches. Please try again shortly.");
		assertThatCode(() -> limiter.requireAllowed(UUID.randomUUID())).doesNotThrowAnyException();

		clock.advance(Duration.ofMinutes(1));
		assertThatCode(() -> limiter.requireAllowed(userId)).doesNotThrowAnyException();
	}

	private static final class MutableClock extends Clock {
		private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
