package com.narayansharma.foodrecommender.catalog.duplicates;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantDuplicateReviewService {
	private static final String SELECT_COLUMNS = """
			SELECT id, location_a_id, location_b_id, match_score, reasons_text, status,
			       canonical_location_id, reviewer_reference, created_at, reviewed_at
			FROM restaurant_duplicate_reviews
			""";

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public RestaurantDuplicateReviewService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public UUID suggest(UUID firstLocationId, UUID secondLocationId, double score, List<String> reasons) {
		validateSuggestion(firstLocationId, secondLocationId, score, reasons);
		UUID locationA = firstLocationId.toString().compareTo(secondLocationId.toString()) < 0
				? firstLocationId : secondLocationId;
		UUID locationB = locationA.equals(firstLocationId) ? secondLocationId : firstLocationId;
		List<UUID> existing = jdbcTemplate.query(
				"""
				SELECT id FROM restaurant_duplicate_reviews
				WHERE location_a_id = ? AND location_b_id = ?
				""",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				locationA,
				locationB);
		if (!existing.isEmpty()) {
			return existing.getFirst();
		}

		UUID reviewId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurant_duplicate_reviews (
				    id, location_a_id, location_b_id, match_score, reasons_text, status, created_at
				) VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
				""",
				reviewId,
				locationA,
				locationB,
				score,
				String.join("\n", reasons),
				Timestamp.from(Instant.now(clock)));
		return reviewId;
	}

	@Transactional(readOnly = true)
	public List<RestaurantDuplicateReview> pendingReviews() {
		return jdbcTemplate.query(
				SELECT_COLUMNS + " WHERE status = 'PENDING' ORDER BY created_at, id",
				this::mapReview);
	}

	@Transactional
	public void merge(UUID reviewId, UUID canonicalLocationId, String reviewerReference) {
		validateReviewer(reviewerReference);
		if (canonicalLocationId == null) {
			throw new IllegalArgumentException("Canonical restaurant location is required");
		}
		RestaurantDuplicateReview review = findForUpdate(reviewId);
		requirePending(review);
		if (!canonicalLocationId.equals(review.locationAId())
				&& !canonicalLocationId.equals(review.locationBId())) {
			throw new IllegalArgumentException("Canonical location must belong to the duplicate review");
		}

		UUID duplicateLocationId = canonicalLocationId.equals(review.locationAId())
				? review.locationBId() : review.locationAId();
		LocationOwnership canonical = findLocationForUpdate(canonicalLocationId);
		LocationOwnership duplicate = findLocationForUpdate(duplicateLocationId);
		if (!"ACTIVE".equals(canonical.status()) || !"ACTIVE".equals(duplicate.status())) {
			throw new IllegalStateException("Only active restaurant locations can be merged");
		}
		requireUnmergedRestaurant(canonical.restaurantId());
		requireUnmergedRestaurant(duplicate.restaurantId());

		jdbcTemplate.update("""
				UPDATE restaurant_locations
				SET status = 'REMOVED', merged_into_id = ?, updated_at = ?
				WHERE id = ?
				""", canonicalLocationId, Timestamp.from(Instant.now(clock)), duplicateLocationId);
		if (!canonical.restaurantId().equals(duplicate.restaurantId())) {
			jdbcTemplate.update("""
					UPDATE restaurant_locations
					SET restaurant_id = ?, updated_at = ?
					WHERE restaurant_id = ? AND id <> ? AND status <> 'REMOVED'
					""",
					canonical.restaurantId(),
					Timestamp.from(Instant.now(clock)),
					duplicate.restaurantId(),
					duplicateLocationId);
			int mergedRestaurants = jdbcTemplate.update("""
					UPDATE restaurants
					SET merged_into_id = ?, updated_at = ?
					WHERE id = ? AND merged_into_id IS NULL
					""",
					canonical.restaurantId(),
					Timestamp.from(Instant.now(clock)),
					duplicate.restaurantId());
			if (mergedRestaurants != 1) {
				throw new IllegalStateException("Duplicate restaurant was already merged");
			}
		}
		completeReview(reviewId, "MERGED", canonicalLocationId, reviewerReference);
	}

	@Transactional
	public void dismiss(UUID reviewId, String reviewerReference) {
		validateReviewer(reviewerReference);
		RestaurantDuplicateReview review = findForUpdate(reviewId);
		requirePending(review);
		completeReview(reviewId, "DISMISSED", null, reviewerReference);
	}

	private void completeReview(
			UUID reviewId,
			String status,
			UUID canonicalLocationId,
			String reviewerReference) {
		jdbcTemplate.update("""
				UPDATE restaurant_duplicate_reviews
				SET status = ?, canonical_location_id = ?, reviewer_reference = ?, reviewed_at = ?
				WHERE id = ?
				""",
				status,
				canonicalLocationId,
				reviewerReference,
				Timestamp.from(Instant.now(clock)),
				reviewId);
	}

	private RestaurantDuplicateReview findForUpdate(UUID reviewId) {
		if (reviewId == null) {
			throw new IllegalArgumentException("Duplicate review ID is required");
		}
		return jdbcTemplate.query(
				SELECT_COLUMNS + " WHERE id = ? FOR UPDATE",
				this::mapReview,
				reviewId).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown duplicate review: " + reviewId));
	}

	private LocationOwnership findLocationForUpdate(UUID locationId) {
		return jdbcTemplate.query("""
				SELECT restaurant_id, status
				FROM restaurant_locations
				WHERE id = ?
				FOR UPDATE
				""",
				(resultSet, rowNumber) -> new LocationOwnership(
						resultSet.getObject("restaurant_id", UUID.class),
						resultSet.getString("status")),
				locationId).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown restaurant location: " + locationId));
	}

	private void requireUnmergedRestaurant(UUID restaurantId) {
		Integer unmerged = jdbcTemplate.queryForObject("""
				SELECT CASE WHEN merged_into_id IS NULL THEN 1 ELSE 0 END
				FROM restaurants
				WHERE id = ?
				FOR UPDATE
				""", Integer.class, restaurantId);
		if (unmerged == null || unmerged != 1) {
			throw new IllegalStateException("Restaurant was already merged");
		}
	}

	private RestaurantDuplicateReview mapReview(ResultSet resultSet, int rowNumber) throws SQLException {
		String reasons = resultSet.getString("reasons_text");
		Timestamp reviewedAt = resultSet.getTimestamp("reviewed_at");
		return new RestaurantDuplicateReview(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("location_a_id", UUID.class),
				resultSet.getObject("location_b_id", UUID.class),
				resultSet.getDouble("match_score"),
				reasons.isEmpty() ? List.of() : Arrays.asList(reasons.split("\n")),
				RestaurantDuplicateReviewStatus.valueOf(resultSet.getString("status")),
				resultSet.getObject("canonical_location_id", UUID.class),
				resultSet.getString("reviewer_reference"),
				resultSet.getTimestamp("created_at").toInstant(),
				reviewedAt == null ? null : reviewedAt.toInstant());
	}

	private void validateSuggestion(
			UUID firstLocationId,
			UUID secondLocationId,
			double score,
			List<String> reasons) {
		if (firstLocationId == null || secondLocationId == null || firstLocationId.equals(secondLocationId)) {
			throw new IllegalArgumentException("Two different restaurant locations are required");
		}
		if (!Double.isFinite(score) || score < 0 || score > 1) {
			throw new IllegalArgumentException("Duplicate match score must be between 0 and 1");
		}
		if (reasons == null || reasons.isEmpty() || reasons.size() > 10
				|| reasons.stream().anyMatch(reason -> reason == null || reason.isBlank()
						|| reason.length() > 100 || reason.contains("\n"))
				|| String.join("\n", reasons).length() > 1000) {
			throw new IllegalArgumentException("Duplicate match reasons are invalid");
		}
	}

	private void validateReviewer(String reviewerReference) {
		if (reviewerReference == null || reviewerReference.isBlank() || reviewerReference.length() > 200) {
			throw new IllegalArgumentException("Reviewer reference is required");
		}
	}

	private void requirePending(RestaurantDuplicateReview review) {
		if (review.status() != RestaurantDuplicateReviewStatus.PENDING) {
			throw new IllegalStateException("Duplicate review is already complete");
		}
	}

	private record LocationOwnership(UUID restaurantId, String status) {
	}
}
