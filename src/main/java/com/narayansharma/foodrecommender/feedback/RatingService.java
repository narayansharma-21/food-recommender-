package com.narayansharma.foodrecommender.feedback;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public RatingService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public UUID create(UUID userId, SaveRatingRequest request) {
		requireValidRequest(request);
		lockActiveUser(userId);
		UUID dishConceptId = findDishConcept(request.menuItemId());
		if (ratingExists(userId, request.menuItemId())) {
			throw new ApiException(
					HttpStatus.CONFLICT,
					"RATING_EXISTS",
					"A rating already exists for this menu item.");
		}

		UUID ratingId = UUID.randomUUID();
		Instant now = clock.instant();
		String comment = cleanComment(request.comment());
		List<String> tags = cleanTags(request.tags());
		jdbcTemplate.update("""
				INSERT INTO ratings (
				    id, user_id, menu_item_id, dish_concept_id, score,
				    would_order_again, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				ratingId,
				userId,
				request.menuItemId(),
				dishConceptId,
				request.score(),
				request.wouldOrderAgain(),
				Timestamp.from(now),
				Timestamp.from(now));
		writeComment(ratingId, comment, now);
		writeTags("rating_tags", ratingId, tags, now);

		writeRevision(ratingId, 1, "CREATED", request, comment, tags, now);
		return ratingId;
	}

	private void requireValidRequest(SaveRatingRequest request) {
		if (request == null || request.menuItemId() == null) {
			throw new IllegalArgumentException("Menu item is required");
		}
		if (request.score() < 1 || request.score() > 5) {
			throw new IllegalArgumentException("Rating score must be between 1 and 5");
		}
	}

	private void lockActiveUser(UUID userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		List<UUID> users = jdbcTemplate.query(
				"SELECT id FROM users WHERE id = ? AND status = 'ACTIVE' FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				userId);
		if (users.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "The active user was not found.");
		}
	}

	private UUID findDishConcept(UUID menuItemId) {
		List<UUID> dishes = jdbcTemplate.query(
				"SELECT dish_concept_id FROM menu_items WHERE id = ?",
				(resultSet, rowNumber) -> resultSet.getObject("dish_concept_id", UUID.class),
				menuItemId);
		if (dishes.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "MENU_ITEM_NOT_FOUND", "The menu item was not found.");
		}
		return dishes.getFirst();
	}

	private boolean ratingExists(UUID userId, UUID menuItemId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM ratings WHERE user_id = ? AND menu_item_id = ?",
				Integer.class,
				userId,
				menuItemId);
		return count != null && count > 0;
	}

	private String cleanComment(String comment) {
		if (comment == null || comment.isBlank()) {
			return null;
		}
		String clean = comment.strip();
		if (clean.length() > 4000) {
			throw new IllegalArgumentException("Rating comment is too long");
		}
		return clean;
	}

	private List<String> cleanTags(List<String> tags) {
		if (tags == null) {
			return List.of();
		}
		List<String> clean = tags.stream()
				.map(tag -> tag == null ? "" : tag.strip().toLowerCase(Locale.ROOT))
				.distinct()
				.sorted()
				.toList();
		if (clean.size() > 10 || clean.stream().anyMatch(tag -> !tag.matches("[a-z][a-z0-9_]{0,49}"))) {
			throw new IllegalArgumentException("Rating tags are invalid");
		}
		return clean;
	}

	private void writeComment(UUID ratingId, String comment, Instant now) {
		if (comment != null) {
			jdbcTemplate.update("""
					INSERT INTO rating_comments (rating_id, original_text, created_at, updated_at)
					VALUES (?, ?, ?, ?)
					""", ratingId, comment, Timestamp.from(now), Timestamp.from(now));
		}
	}

	private void writeRevision(
			UUID ratingId,
			int revisionNumber,
			String changeType,
			SaveRatingRequest request,
			String comment,
			List<String> tags,
			Instant now) {
		UUID revisionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO rating_revisions (
				    id, rating_id, revision_number, change_type, score,
				    would_order_again, original_comment, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				revisionId,
				ratingId,
				revisionNumber,
				changeType,
				request.score(),
				request.wouldOrderAgain(),
				comment,
				Timestamp.from(now));
		writeTags("rating_revision_tags", revisionId, tags, now);
	}

	private void writeTags(String table, UUID id, List<String> tags, Instant now) {
		for (String tag : tags) {
			if ("rating_tags".equals(table)) {
				jdbcTemplate.update(
						"INSERT INTO rating_tags (rating_id, tag_key, created_at) VALUES (?, ?, ?)",
						id,
						tag,
						Timestamp.from(now));
			} else {
				jdbcTemplate.update(
						"INSERT INTO rating_revision_tags (revision_id, tag_key) VALUES (?, ?)", id, tag);
			}
		}
	}
}
