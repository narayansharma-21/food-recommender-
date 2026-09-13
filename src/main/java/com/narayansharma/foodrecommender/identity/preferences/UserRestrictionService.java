package com.narayansharma.foodrecommender.identity.preferences;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRestrictionService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public UserRestrictionService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	public List<UserRestriction> active(UUID userId) {
		requireActiveUser(userId, false);
		return jdbcTemplate.query("""
				SELECT id, restriction_type, restriction_key, display_name, active
				FROM user_restrictions
				WHERE user_id = ? AND active = TRUE
				ORDER BY restriction_type, display_name, id
				""", this::mapRestriction, userId);
	}

	@Transactional
	public UserRestriction set(
			UUID userId,
			RestrictionType type,
			String key,
			String displayName,
			boolean active) {
		if (type == null) {
			throw new IllegalArgumentException("Restriction type is required");
		}
		String cleanKey = normalizeKey(key);
		String cleanDisplayName = requireDisplayName(displayName);
		requireActiveUser(userId, true);
		Instant now = clock.instant();
		List<UserRestriction> existing = find(userId, type, cleanKey);
		if (existing.isEmpty()) {
			UUID restrictionId = UUID.randomUUID();
			jdbcTemplate.update("""
					INSERT INTO user_restrictions (
					    id, user_id, restriction_type, restriction_key,
					    display_name, active, created_at, updated_at
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""",
					restrictionId,
					userId,
					type.name(),
					cleanKey,
					cleanDisplayName,
					active,
					Timestamp.from(now),
					Timestamp.from(now));
			return new UserRestriction(restrictionId, type, cleanKey, cleanDisplayName, active);
		}

		UserRestriction restriction = existing.getFirst();
		jdbcTemplate.update("""
				UPDATE user_restrictions
				SET display_name = ?, active = ?, updated_at = ?
				WHERE id = ?
				""",
				cleanDisplayName,
				active,
				Timestamp.from(now),
				restriction.id());
		return new UserRestriction(restriction.id(), type, cleanKey, cleanDisplayName, active);
	}

	private List<UserRestriction> find(UUID userId, RestrictionType type, String key) {
		return jdbcTemplate.query("""
				SELECT id, restriction_type, restriction_key, display_name, active
				FROM user_restrictions
				WHERE user_id = ? AND restriction_type = ? AND restriction_key = ?
				""", this::mapRestriction, userId, type.name(), key);
	}

	private UserRestriction mapRestriction(java.sql.ResultSet resultSet, int rowNumber)
			throws java.sql.SQLException {
		return new UserRestriction(
				resultSet.getObject("id", UUID.class),
				RestrictionType.valueOf(resultSet.getString("restriction_type")),
				resultSet.getString("restriction_key"),
				resultSet.getString("display_name"),
				resultSet.getBoolean("active"));
	}

	private void requireActiveUser(UUID userId, boolean lock) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		String suffix = lock ? " FOR UPDATE" : "";
		List<UUID> users = jdbcTemplate.query(
				"SELECT id FROM users WHERE id = ? AND status = 'ACTIVE'" + suffix,
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				userId);
		if (users.isEmpty()) {
			throw new IllegalArgumentException("Unknown active user: " + userId);
		}
	}

	private String normalizeKey(String key) {
		if (key == null) {
			throw new IllegalArgumentException("Restriction key is required");
		}
		String clean = key.strip().toLowerCase(Locale.ROOT).replace('-', '_');
		if (!clean.matches("[a-z][a-z0-9_]{0,99}")) {
			throw new IllegalArgumentException("Restriction key is invalid");
		}
		return clean;
	}

	private String requireDisplayName(String displayName) {
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Restriction display name is required");
		}
		String clean = displayName.strip();
		if (clean.length() > 200) {
			throw new IllegalArgumentException("Restriction display name is too long");
		}
		return clean;
	}
}
