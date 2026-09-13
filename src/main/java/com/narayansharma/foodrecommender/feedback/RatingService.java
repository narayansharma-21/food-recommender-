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
	private final RatingQueryService ratingQueryService;
	private final CommentTraitExtractor traitExtractor;

	public RatingService(
			JdbcTemplate jdbcTemplate,
			Clock clock,
			RatingQueryService ratingQueryService,
			CommentTraitExtractor traitExtractor) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.ratingQueryService = ratingQueryService;
		this.traitExtractor = traitExtractor;
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

	@Transactional
	public void update(UUID userId, UUID ratingId, SaveRatingRequest request) {
		requireValidRequest(request);
		lockActiveUser(userId);
		UUID savedMenuItemId = findOwnedActiveRatingMenuItem(userId, ratingId);
		if (!savedMenuItemId.equals(request.menuItemId())) {
			throw new IllegalArgumentException("A rating cannot be moved to another menu item");
		}

		Instant now = clock.instant();
		String comment = cleanComment(request.comment());
		List<String> tags = cleanTags(request.tags());
		jdbcTemplate.update("""
				UPDATE ratings
				SET score = ?, would_order_again = ?, updated_at = ?
				WHERE id = ?
				""", request.score(), request.wouldOrderAgain(), Timestamp.from(now), ratingId);
		jdbcTemplate.update("DELETE FROM rating_comments WHERE rating_id = ?", ratingId);
		writeComment(ratingId, comment, now);
		jdbcTemplate.update("DELETE FROM rating_tags WHERE rating_id = ?", ratingId);
		writeTags("rating_tags", ratingId, tags, now);
		writeRevision(ratingId, nextRevisionNumber(ratingId), "UPDATED", request, comment, tags, now);
	}

	@Transactional
	public void delete(UUID userId, UUID ratingId) {
		lockActiveUser(userId);
		findOwnedActiveRatingMenuItem(userId, ratingId);
		RatingView current = ratingQueryService.get(userId, ratingId);
		Instant now = clock.instant();
		jdbcTemplate.update(
				"UPDATE ratings SET deleted_at = ?, updated_at = ? WHERE id = ?",
				Timestamp.from(now),
				Timestamp.from(now),
				ratingId);
		jdbcTemplate.update("DELETE FROM rating_comments WHERE rating_id = ?", ratingId);
		jdbcTemplate.update("DELETE FROM rating_tags WHERE rating_id = ?", ratingId);
		SaveRatingRequest snapshot = new SaveRatingRequest(
				current.menuItemId(),
				current.score(),
				current.wouldOrderAgain(),
				current.comment(),
				current.tags());
		writeRevision(
				ratingId,
				nextRevisionNumber(ratingId),
				"DELETED",
				snapshot,
				current.comment(),
				current.tags(),
				now);
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

	private UUID findOwnedActiveRatingMenuItem(UUID userId, UUID ratingId) {
		List<UUID> menuItems = jdbcTemplate.query(
				"SELECT menu_item_id FROM ratings WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
				(resultSet, rowNumber) -> resultSet.getObject("menu_item_id", UUID.class),
				ratingId,
				userId);
		if (menuItems.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "RATING_NOT_FOUND", "The rating was not found.");
		}
		return menuItems.getFirst();
	}

	private int nextRevisionNumber(UUID ratingId) {
		Integer number = jdbcTemplate.queryForObject(
				"SELECT COALESCE(MAX(revision_number), 0) + 1 FROM rating_revisions WHERE rating_id = ?",
				Integer.class,
				ratingId);
		return number == null ? 1 : number;
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
		writeTraitSignals(revisionId, comment, now);
	}

	private void writeTraitSignals(UUID revisionId, String comment, Instant now) {
		for (ExtractedTraitSignal signal : traitExtractor.extract(comment)) {
			jdbcTemplate.update("""
					INSERT INTO rating_trait_signals (
					    id, revision_id, trait_key, sentiment, confidence,
					    evidence_text, extractor_version, created_at
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""",
					UUID.randomUUID(),
					revisionId,
					signal.traitKey(),
					signal.sentiment(),
					signal.confidence(),
					signal.evidenceText(),
					signal.extractorVersion(),
					Timestamp.from(now));
		}
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
