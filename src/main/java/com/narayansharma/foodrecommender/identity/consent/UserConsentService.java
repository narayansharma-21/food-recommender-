package com.narayansharma.foodrecommender.identity.consent;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserConsentService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public UserConsentService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public UserConsent record(
			UUID userId,
			ConsentType type,
			boolean granted,
			String policyVersion) {
		if (type == null) {
			throw new IllegalArgumentException("Consent type is required");
		}
		String cleanPolicyVersion = validatePolicyVersion(policyVersion);
		lockActiveUser(userId);
		Integer latestSequence = jdbcTemplate.queryForObject("""
				SELECT COALESCE(MAX(event_sequence), 0)
				FROM user_consent_events
				WHERE user_id = ? AND consent_type = ?
				""", Integer.class, userId, type.name());
		int sequence = (latestSequence == null ? 0 : latestSequence) + 1;
		UUID eventId = UUID.randomUUID();
		Instant recordedAt = clock.instant();
		jdbcTemplate.update("""
				INSERT INTO user_consent_events (
				    id, user_id, consent_type, event_sequence,
				    granted, policy_version, recorded_at
				) VALUES (?, ?, ?, ?, ?, ?, ?)
				""",
				eventId,
				userId,
				type.name(),
				sequence,
				granted,
				cleanPolicyVersion,
				Timestamp.from(recordedAt));
		return new UserConsent(eventId, type, sequence, granted, cleanPolicyVersion, recordedAt);
	}

	public Optional<UserConsent> current(UUID userId, ConsentType type) {
		if (userId == null || type == null) {
			throw new IllegalArgumentException("User ID and consent type are required");
		}
		List<UserConsent> events = jdbcTemplate.query("""
				SELECT id, consent_type, event_sequence, granted, policy_version, recorded_at
				FROM user_consent_events
				WHERE user_id = ? AND consent_type = ?
				ORDER BY event_sequence DESC
				LIMIT 1
				""", this::mapConsent, userId, type.name());
		return events.stream().findFirst();
	}

	private UserConsent mapConsent(java.sql.ResultSet resultSet, int rowNumber)
			throws java.sql.SQLException {
		return new UserConsent(
				resultSet.getObject("id", UUID.class),
				ConsentType.valueOf(resultSet.getString("consent_type")),
				resultSet.getInt("event_sequence"),
				resultSet.getBoolean("granted"),
				resultSet.getString("policy_version"),
				resultSet.getTimestamp("recorded_at").toInstant());
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
			throw new IllegalArgumentException("Unknown active user: " + userId);
		}
	}

	private String validatePolicyVersion(String policyVersion) {
		if (policyVersion == null || policyVersion.isBlank()) {
			throw new IllegalArgumentException("Consent policy version is required");
		}
		String clean = policyVersion.strip();
		if (!clean.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,49}")) {
			throw new IllegalArgumentException("Consent policy version is invalid");
		}
		return clean;
	}
}
