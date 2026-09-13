package com.narayansharma.foodrecommender.feedback;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeedbackChangedPublisher {
	private final JdbcTemplate jdbcTemplate;

	public FeedbackChangedPublisher(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void publish(
			UUID userId,
			UUID ratingId,
			UUID revisionId,
			String changeType,
			Instant occurredAt) {
		jdbcTemplate.update("""
				INSERT INTO feedback_change_events (
				    id, user_id, rating_id, revision_id, change_type, occurred_at
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				userId,
				ratingId,
				revisionId,
				changeType,
				Timestamp.from(occurredAt));
	}
}
