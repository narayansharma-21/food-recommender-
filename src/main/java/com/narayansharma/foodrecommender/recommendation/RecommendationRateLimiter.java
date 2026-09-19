package com.narayansharma.foodrecommender.recommendation;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RecommendationRateLimiter {
	private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();
	private final Clock clock;
	private final int requestLimit;
	private final Duration windowDuration;

	public RecommendationRateLimiter(
			Clock clock,
			@Value("${recommendation.rate-limit.requests:30}") int requestLimit,
			@Value("${recommendation.rate-limit.window:PT1M}") Duration windowDuration) {
		if (requestLimit < 1 || windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) {
			throw new IllegalArgumentException("Recommendation rate limit configuration is invalid");
		}
		this.clock = clock;
		this.requestLimit = requestLimit;
		this.windowDuration = windowDuration;
	}

	public void requireAllowed(UUID userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		Instant now = clock.instant();
		AtomicBoolean rejected = new AtomicBoolean();
		windows.compute(userId, (ignored, current) -> {
			if (current == null || !now.isBefore(current.startedAt().plus(windowDuration))) {
				return new Window(now, 1);
			}
			if (current.requestCount() >= requestLimit) {
				rejected.set(true);
				return current;
			}
			return new Window(current.startedAt(), current.requestCount() + 1);
		});
		if (rejected.get()) {
			throw new ApiException(
					HttpStatus.TOO_MANY_REQUESTS,
					"RATE_LIMIT_EXCEEDED",
					"Too many recommendation requests. Please try again shortly.");
		}
	}

	private record Window(Instant startedAt, int requestCount) {
	}
}
