package com.narayansharma.foodrecommender.feedback;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingModerationService {
	private final JdbcTemplate jdbcTemplate;
	private final RatingQueryService ratingQueryService;
	private final Clock clock;

	public RatingModerationService(
			JdbcTemplate jdbcTemplate,
			RatingQueryService ratingQueryService,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.ratingQueryService = ratingQueryService;
		this.clock = clock;
	}

	@Transactional
	public ReportRatingResponse report(UUID userId, UUID ratingId, ReportRatingRequest request) {
		ratingQueryService.get(userId, ratingId);
		String reason = cleanReason(request);
		String details = cleanDetails(request.details());
		UUID caseId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO rating_moderation_cases (
				    id, rating_id, reporter_user_id, source_type,
				    reason_code, details, status, created_at
				) VALUES (?, ?, ?, 'USER_REPORT', ?, ?, 'OPEN', ?)
				""",
				caseId,
				ratingId,
				userId,
				reason,
				details,
				Timestamp.from(clock.instant()));
		return new ReportRatingResponse(caseId, "OPEN");
	}

	private String cleanReason(ReportRatingRequest request) {
		if (request == null || request.reasonCode() == null) {
			throw new IllegalArgumentException("Report reason is required");
		}
		String clean = request.reasonCode().strip().toUpperCase(Locale.ROOT);
		if (!clean.matches("[A-Z][A-Z0-9_]{0,49}")) {
			throw new IllegalArgumentException("Report reason is invalid");
		}
		return clean;
	}

	private String cleanDetails(String details) {
		if (details == null || details.isBlank()) {
			return null;
		}
		String clean = details.strip();
		if (clean.length() > 1000) {
			throw new IllegalArgumentException("Report details are too long");
		}
		return clean;
	}
}
