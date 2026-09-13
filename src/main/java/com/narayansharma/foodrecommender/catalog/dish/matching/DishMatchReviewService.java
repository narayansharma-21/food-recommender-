package com.narayansharma.foodrecommender.catalog.dish.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishMatchReviewService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final double reviewMinimumConfidence;
	private final double autoLinkMinimumConfidence;

	public DishMatchReviewService(
			JdbcTemplate jdbcTemplate,
			Clock clock,
			@Value("${dish.matching.review-minimum-confidence:0.5}") double reviewMinimumConfidence,
			@Value("${dish.matching.auto-link-minimum-confidence:0.9}") double autoLinkMinimumConfidence) {
		if (!Double.isFinite(reviewMinimumConfidence)
				|| !Double.isFinite(autoLinkMinimumConfidence)
				|| reviewMinimumConfidence < 0
				|| reviewMinimumConfidence >= autoLinkMinimumConfidence
				|| autoLinkMinimumConfidence > 1) {
			throw new IllegalArgumentException("Dish match confidence thresholds are invalid");
		}
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.reviewMinimumConfidence = reviewMinimumConfidence;
		this.autoLinkMinimumConfidence = autoLinkMinimumConfidence;
	}

	@Transactional
	public StoredDishMatchReview queueUncertain(
			UUID menuItemId,
			UUID suggestedDishConceptId,
			double confidence,
			String matchMethod) {
		if (!Double.isFinite(confidence)) {
			throw new IllegalArgumentException("Only uncertain dish matches can enter review");
		}
		BigDecimal storedConfidence = BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP);
		validateCandidate(menuItemId, suggestedDishConceptId, storedConfidence.doubleValue(), matchMethod);
		lockMenuItem(menuItemId);
		requireDishConcept(suggestedDishConceptId);
		List<StoredDishMatchReview> existing = findCandidate(
				menuItemId,
				suggestedDishConceptId,
				matchMethod);
		if (!existing.isEmpty()) {
			return withCreated(existing.getFirst(), false);
		}

		UUID reviewId = UUID.randomUUID();
		Instant createdAt = clock.instant();
		jdbcTemplate.update("""
				INSERT INTO dish_match_reviews (
				    id, menu_item_id, suggested_dish_concept_id, confidence,
				    match_method, status, created_at
				) VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
				""",
				reviewId,
				menuItemId,
				suggestedDishConceptId,
				storedConfidence,
				matchMethod,
				Timestamp.from(createdAt));
		return new StoredDishMatchReview(
				reviewId,
				menuItemId,
				suggestedDishConceptId,
				storedConfidence.doubleValue(),
				matchMethod,
				"PENDING",
				null,
				null,
				createdAt,
				null,
				true);
	}

	@Transactional
	public StoredDishMatchReview resolve(
			UUID reviewId,
			DishMatchReviewDecision decision,
			String reviewerReference,
			String resolutionNote) {
		validateResolution(reviewId, decision, reviewerReference, resolutionNote);
		UUID menuItemId = findReviewItemId(reviewId);
		lockMenuItem(menuItemId);
		StoredDishMatchReview review = lockReview(reviewId);
		String targetStatus = decision == DishMatchReviewDecision.APPROVE ? "APPROVED" : "REJECTED";
		if (!"PENDING".equals(review.status())) {
			if (targetStatus.equals(review.status())) {
				return withCreated(review, false);
			}
			throw new IllegalStateException("Dish match review is already resolved");
		}

		if (decision == DishMatchReviewDecision.APPROVE) {
			int updated = jdbcTemplate.update("""
					UPDATE menu_items
					SET dish_concept_id = ?
					WHERE id = ? AND (dish_concept_id IS NULL OR dish_concept_id = ?)
					""",
					review.suggestedDishConceptId(),
					review.menuItemId(),
					review.suggestedDishConceptId());
			if (updated != 1) {
				throw new IllegalStateException("Menu item is already linked to another dish concept");
			}
		}

		Instant resolvedAt = clock.instant();
		jdbcTemplate.update("""
				UPDATE dish_match_reviews
				SET status = ?, reviewer_reference = ?, resolution_note = ?, resolved_at = ?
				WHERE id = ?
				""",
				targetStatus,
				reviewerReference,
				resolutionNote,
				Timestamp.from(resolvedAt),
				reviewId);
		if (decision == DishMatchReviewDecision.APPROVE) {
			rejectCompetingCandidates(review, reviewerReference, resolvedAt);
		}
		return new StoredDishMatchReview(
				review.id(),
				review.menuItemId(),
				review.suggestedDishConceptId(),
				review.confidence(),
				review.matchMethod(),
				targetStatus,
				reviewerReference,
				resolutionNote,
				review.createdAt(),
				resolvedAt,
				false);
	}

	private void rejectCompetingCandidates(
			StoredDishMatchReview approved,
			String reviewerReference,
			Instant resolvedAt) {
		jdbcTemplate.update("""
				UPDATE dish_match_reviews
				SET status = 'REJECTED', reviewer_reference = ?,
				    resolution_note = ?, resolved_at = ?
				WHERE menu_item_id = ? AND id <> ? AND status = 'PENDING'
				""",
				reviewerReference,
				"Superseded by approved review " + approved.id(),
				Timestamp.from(resolvedAt),
				approved.menuItemId(),
				approved.id());
	}

	public List<StoredDishMatchReview> pending(int limit) {
		if (limit < 1 || limit > 100) {
			throw new IllegalArgumentException("Dish match review limit must be between 1 and 100");
		}
		return jdbcTemplate.query("""
				SELECT *
				FROM dish_match_reviews
				WHERE status = 'PENDING'
				ORDER BY created_at, id
				LIMIT ?
				""", this::mapReview, limit);
	}

	private void validateCandidate(
			UUID menuItemId,
			UUID suggestedDishConceptId,
			double confidence,
			String matchMethod) {
		if (menuItemId == null || suggestedDishConceptId == null) {
			throw new IllegalArgumentException("Menu item and suggested dish concept are required");
		}
		if (!Double.isFinite(confidence)
				|| confidence < reviewMinimumConfidence
				|| confidence >= autoLinkMinimumConfidence) {
			throw new IllegalArgumentException("Only uncertain dish matches can enter review");
		}
		if (matchMethod == null || !matchMethod.matches("[a-z][a-z0-9_-]{0,99}")) {
			throw new IllegalArgumentException("Dish match method is invalid");
		}
	}

	private void validateResolution(
			UUID reviewId,
			DishMatchReviewDecision decision,
			String reviewerReference,
			String resolutionNote) {
		if (reviewId == null || decision == null) {
			throw new IllegalArgumentException("Dish match review and decision are required");
		}
		if (reviewerReference == null
				|| reviewerReference.isBlank()
				|| reviewerReference.length() > 200) {
			throw new IllegalArgumentException("Reviewer reference is invalid");
		}
		if (resolutionNote != null && resolutionNote.length() > 1_000) {
			throw new IllegalArgumentException("Dish match resolution note is too long");
		}
	}

	private void lockMenuItem(UUID menuItemId) {
		List<UUID> items = jdbcTemplate.query(
				"SELECT id FROM menu_items WHERE id = ? FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				menuItemId);
		if (items.isEmpty()) {
			throw new IllegalArgumentException("Unknown menu item: " + menuItemId);
		}
	}

	private void requireDishConcept(UUID dishConceptId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM dish_concepts WHERE id = ?",
				Integer.class,
				dishConceptId);
		if (count == null || count != 1) {
			throw new IllegalArgumentException("Unknown dish concept: " + dishConceptId);
		}
	}

	private UUID findReviewItemId(UUID reviewId) {
		List<UUID> itemIds = jdbcTemplate.query(
				"SELECT menu_item_id FROM dish_match_reviews WHERE id = ?",
				(resultSet, rowNumber) -> resultSet.getObject("menu_item_id", UUID.class),
				reviewId);
		return itemIds.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown dish match review: " + reviewId));
	}

	private StoredDishMatchReview lockReview(UUID reviewId) {
		List<StoredDishMatchReview> reviews = jdbcTemplate.query(
				"SELECT * FROM dish_match_reviews WHERE id = ? FOR UPDATE",
				this::mapReview,
				reviewId);
		return reviews.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown dish match review: " + reviewId));
	}

	private List<StoredDishMatchReview> findCandidate(
			UUID menuItemId,
			UUID suggestedDishConceptId,
			String matchMethod) {
		return jdbcTemplate.query("""
				SELECT *
				FROM dish_match_reviews
				WHERE menu_item_id = ? AND suggested_dish_concept_id = ? AND match_method = ?
				""",
				this::mapReview,
				menuItemId,
				suggestedDishConceptId,
				matchMethod);
	}

	private StoredDishMatchReview mapReview(java.sql.ResultSet resultSet, int rowNumber)
			throws java.sql.SQLException {
		Timestamp resolvedAt = resultSet.getTimestamp("resolved_at");
		return new StoredDishMatchReview(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("menu_item_id", UUID.class),
				resultSet.getObject("suggested_dish_concept_id", UUID.class),
				resultSet.getDouble("confidence"),
				resultSet.getString("match_method"),
				resultSet.getString("status"),
				resultSet.getString("reviewer_reference"),
				resultSet.getString("resolution_note"),
				resultSet.getTimestamp("created_at").toInstant(),
				resolvedAt == null ? null : resolvedAt.toInstant(),
				false);
	}

	private StoredDishMatchReview withCreated(StoredDishMatchReview review, boolean created) {
		return new StoredDishMatchReview(
				review.id(),
				review.menuItemId(),
				review.suggestedDishConceptId(),
				review.confidence(),
				review.matchMethod(),
				review.status(),
				review.reviewerReference(),
				review.resolutionNote(),
				review.createdAt(),
				review.resolvedAt(),
				created);
	}
}
