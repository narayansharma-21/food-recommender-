package com.narayansharma.foodrecommender.catalog.changes;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantChangeRequestService {
	private static final String SELECT_COLUMNS = """
			SELECT id, location_id, requester_reference, request_kind, correction_field,
			       proposed_value, reason, evidence_url, status, reviewer_reference,
			       resolution_note, created_at, reviewed_at
			FROM restaurant_change_requests
			""";

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public RestaurantChangeRequestService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public UUID submitCorrection(
			UUID locationId,
			String requesterReference,
			RestaurantCorrectionField field,
			String proposedValue,
			String reason,
			URI evidenceUrl) {
		if (field == null || proposedValue == null || proposedValue.isBlank() || proposedValue.length() > 1000) {
			throw new IllegalArgumentException("Correction field and proposed value are required");
		}
		return submit(
				locationId,
				requesterReference,
				RestaurantChangeRequestKind.CORRECTION,
				field,
				proposedValue,
				reason,
				evidenceUrl);
	}

	@Transactional
	public UUID submitRemoval(
			UUID locationId,
			String requesterReference,
			String reason,
			URI evidenceUrl) {
		return submit(
				locationId,
				requesterReference,
				RestaurantChangeRequestKind.REMOVAL,
				null,
				null,
				reason,
				evidenceUrl);
	}

	@Transactional(readOnly = true)
	public List<RestaurantChangeRequest> pendingRequests() {
		return jdbcTemplate.query(
				SELECT_COLUMNS + " WHERE status = 'PENDING' ORDER BY created_at, id LIMIT 100",
				this::mapRequest);
	}

	@Transactional
	public void accept(UUID requestId, String reviewerReference, String resolutionNote) {
		complete(requestId, RestaurantChangeRequestStatus.ACCEPTED, reviewerReference, resolutionNote);
	}

	@Transactional
	public void reject(UUID requestId, String reviewerReference, String resolutionNote) {
		complete(requestId, RestaurantChangeRequestStatus.REJECTED, reviewerReference, resolutionNote);
	}

	private UUID submit(
			UUID locationId,
			String requesterReference,
			RestaurantChangeRequestKind kind,
			RestaurantCorrectionField field,
			String proposedValue,
			String reason,
			URI evidenceUrl) {
		validateSubmission(locationId, requesterReference, reason, evidenceUrl);
		UUID requestId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO restaurant_change_requests (
				    id, location_id, requester_reference, request_kind, correction_field,
				    proposed_value, reason, evidence_url, status, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
				""",
				requestId,
				locationId,
				requesterReference,
				kind.name(),
				field == null ? null : field.name(),
				proposedValue,
				reason,
				evidenceUrl == null ? null : evidenceUrl.toString(),
				Timestamp.from(Instant.now(clock)));
		return requestId;
	}

	private void complete(
			UUID requestId,
			RestaurantChangeRequestStatus status,
			String reviewerReference,
			String resolutionNote) {
		validateReview(requestId, reviewerReference, resolutionNote);
		List<RestaurantChangeRequestStatus> currentStatuses = jdbcTemplate.query(
				"SELECT status FROM restaurant_change_requests WHERE id = ? FOR UPDATE",
				(resultSet, rowNumber) -> RestaurantChangeRequestStatus.valueOf(resultSet.getString("status")),
				requestId);
		if (currentStatuses.isEmpty()) {
			throw new IllegalArgumentException("Unknown restaurant change request: " + requestId);
		}
		if (currentStatuses.getFirst() != RestaurantChangeRequestStatus.PENDING) {
			throw new IllegalStateException("Restaurant change request is already complete");
		}
		jdbcTemplate.update("""
				UPDATE restaurant_change_requests
				SET status = ?, reviewer_reference = ?, resolution_note = ?, reviewed_at = ?
				WHERE id = ?
				""",
				status.name(),
				reviewerReference,
				resolutionNote,
				Timestamp.from(Instant.now(clock)),
				requestId);
	}

	private RestaurantChangeRequest mapRequest(ResultSet resultSet, int rowNumber) throws SQLException {
		String field = resultSet.getString("correction_field");
		String evidenceUrl = resultSet.getString("evidence_url");
		Timestamp reviewedAt = resultSet.getTimestamp("reviewed_at");
		return new RestaurantChangeRequest(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("location_id", UUID.class),
				resultSet.getString("requester_reference"),
				RestaurantChangeRequestKind.valueOf(resultSet.getString("request_kind")),
				field == null ? null : RestaurantCorrectionField.valueOf(field),
				resultSet.getString("proposed_value"),
				resultSet.getString("reason"),
				evidenceUrl == null ? null : URI.create(evidenceUrl),
				RestaurantChangeRequestStatus.valueOf(resultSet.getString("status")),
				resultSet.getString("reviewer_reference"),
				resultSet.getString("resolution_note"),
				resultSet.getTimestamp("created_at").toInstant(),
				reviewedAt == null ? null : reviewedAt.toInstant());
	}

	private void validateSubmission(
			UUID locationId,
			String requesterReference,
			String reason,
			URI evidenceUrl) {
		if (locationId == null) {
			throw new IllegalArgumentException("Restaurant location is required");
		}
		Integer activeLocation = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM restaurant_locations
				WHERE id = ? AND status = 'ACTIVE' AND merged_into_id IS NULL
				""", Integer.class, locationId);
		if (activeLocation == null || activeLocation != 1) {
			throw new IllegalArgumentException("Change request requires an active restaurant location");
		}
		validateActorReference(requesterReference, "Requester");
		if (reason == null || reason.isBlank() || reason.length() > 2000) {
			throw new IllegalArgumentException("Change reason is required");
		}
		if (evidenceUrl != null && (!evidenceUrl.isAbsolute()
				|| evidenceUrl.getHost() == null
				|| !("http".equalsIgnoreCase(evidenceUrl.getScheme())
				|| "https".equalsIgnoreCase(evidenceUrl.getScheme()))
				|| evidenceUrl.toString().length() > 500)) {
			throw new IllegalArgumentException("Evidence URL must be an absolute HTTP or HTTPS URL");
		}
	}

	private void validateReview(UUID requestId, String reviewerReference, String resolutionNote) {
		if (requestId == null) {
			throw new IllegalArgumentException("Restaurant change request ID is required");
		}
		validateActorReference(reviewerReference, "Reviewer");
		if (resolutionNote != null && resolutionNote.length() > 2000) {
			throw new IllegalArgumentException("Resolution note is too long");
		}
	}

	private void validateActorReference(String reference, String label) {
		if (reference == null || reference.isBlank() || reference.length() > 200) {
			throw new IllegalArgumentException(label + " reference is required");
		}
	}
}
