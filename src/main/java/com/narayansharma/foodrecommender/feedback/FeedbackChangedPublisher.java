package com.narayansharma.foodrecommender.feedback;

import com.narayansharma.foodrecommender.taste.TasteProfileCalculator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeedbackChangedPublisher {
	private final JdbcTemplate jdbcTemplate;
	private final TasteProfileCalculator profileCalculator;

	public FeedbackChangedPublisher(JdbcTemplate jdbcTemplate, TasteProfileCalculator profileCalculator) {
		this.jdbcTemplate = jdbcTemplate;
		this.profileCalculator = profileCalculator;
	}

	public void publish(
			UUID userId,
			UUID ratingId,
			UUID revisionId,
			String changeType,
			Instant occurredAt) {
		UUID eventId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO feedback_change_events (
				    id, user_id, rating_id, revision_id, change_type, occurred_at
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				eventId,
				userId,
				ratingId,
				revisionId,
				changeType,
				Timestamp.from(occurredAt));
		profileCalculator.recalculate(userId);
		jdbcTemplate.update(
				"UPDATE feedback_change_events SET processed_at = ? WHERE id = ?",
				Timestamp.from(occurredAt),
				eventId);
	}
}
