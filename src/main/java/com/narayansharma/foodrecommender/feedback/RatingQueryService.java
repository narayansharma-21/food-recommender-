package com.narayansharma.foodrecommender.feedback;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RatingQueryService {
	private final JdbcTemplate jdbcTemplate;

	public RatingQueryService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public RatingView get(UUID userId, UUID ratingId) {
		List<RatingView> ratings = jdbcTemplate.query("""
				SELECT r.id, r.menu_item_id, r.dish_concept_id, r.score,
				       r.would_order_again, c.original_text, r.created_at, r.updated_at
				FROM ratings r
				LEFT JOIN rating_comments c ON c.rating_id = r.id
				WHERE r.id = ? AND r.user_id = ? AND r.deleted_at IS NULL
				""", (resultSet, rowNumber) -> new RatingView(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("menu_item_id", UUID.class),
				resultSet.getObject("dish_concept_id", UUID.class),
				resultSet.getInt("score"),
				resultSet.getObject("would_order_again", Boolean.class),
				resultSet.getString("original_text"),
				List.of(),
				resultSet.getTimestamp("created_at").toInstant(),
				resultSet.getTimestamp("updated_at").toInstant()), ratingId, userId);
		if (ratings.isEmpty()) {
			throw notFound();
		}
		RatingView rating = ratings.getFirst();
		return new RatingView(
				rating.id(),
				rating.menuItemId(),
				rating.dishConceptId(),
				rating.score(),
				rating.wouldOrderAgain(),
				rating.comment(),
				tags("rating_tags", "rating_id", ratingId),
				rating.createdAt(),
				rating.updatedAt());
	}

	public List<RatingRevisionView> history(UUID userId, UUID ratingId) {
		requireOwnedRating(userId, ratingId);
		List<RatingRevisionView> revisions = jdbcTemplate.query("""
				SELECT revision.id, revision.revision_number, revision.change_type,
				       revision.score, revision.would_order_again,
				       revision.original_comment, revision.created_at
				FROM rating_revisions revision
				WHERE revision.rating_id = ?
				ORDER BY revision.revision_number DESC
				""", (resultSet, rowNumber) -> {
			UUID revisionId = resultSet.getObject("id", UUID.class);
			return new RatingRevisionView(
					revisionId,
					resultSet.getInt("revision_number"),
					resultSet.getString("change_type"),
					resultSet.getInt("score"),
					resultSet.getObject("would_order_again", Boolean.class),
					resultSet.getString("original_comment"),
					List.of(),
					resultSet.getTimestamp("created_at").toInstant());
		}, ratingId);
		return revisions.stream()
				.map(revision -> new RatingRevisionView(
						revision.id(),
						revision.revisionNumber(),
						revision.changeType(),
						revision.score(),
						revision.wouldOrderAgain(),
						revision.comment(),
						tags("rating_revision_tags", "revision_id", revision.id()),
						revision.createdAt()))
				.toList();
	}

	private void requireOwnedRating(UUID userId, UUID ratingId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM ratings WHERE id = ? AND user_id = ?",
				Integer.class,
				ratingId,
				userId);
		if (count == null || count == 0) {
			throw notFound();
		}
	}

	private List<String> tags(String table, String idColumn, UUID id) {
		return jdbcTemplate.queryForList(
				"SELECT tag_key FROM " + table + " WHERE " + idColumn + " = ? ORDER BY tag_key",
				String.class,
				id);
	}

	private ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "RATING_NOT_FOUND", "The rating was not found.");
	}
}
