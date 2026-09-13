package com.narayansharma.foodrecommender.identity.profile;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public UserProfileService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	public UserProfile get(UUID userId) {
		requireUserId(userId);
		return findActive(userId).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown active user: " + userId));
	}

	@Transactional
	public UserProfile update(
			UUID userId,
			String displayName,
			String homeCity,
			String preferredLocale) {
		requireUserId(userId);
		String cleanDisplayName = optionalText(displayName, 100, "Display name");
		String cleanHomeCity = optionalText(homeCity, 100, "Home city");
		String cleanLocale = requiredText(preferredLocale, 20, "Preferred locale");
		lockActiveUser(userId);
		int updated = jdbcTemplate.update("""
				UPDATE user_profiles
				SET display_name = ?, home_city = ?, preferred_locale = ?, updated_at = ?
				WHERE user_id = ?
				""",
				cleanDisplayName,
				cleanHomeCity,
				cleanLocale,
				Timestamp.from(clock.instant()),
				userId);
		if (updated != 1) {
			throw new IllegalStateException("Active user profile is missing");
		}
		return new UserProfile(userId, cleanDisplayName, cleanHomeCity, cleanLocale);
	}

	private List<UserProfile> findActive(UUID userId) {
		return jdbcTemplate.query("""
				SELECT profile.user_id, profile.display_name, profile.home_city, profile.preferred_locale
				FROM user_profiles profile
				JOIN users ON users.id = profile.user_id
				WHERE profile.user_id = ? AND users.status = 'ACTIVE'
				""",
				(resultSet, rowNumber) -> new UserProfile(
						resultSet.getObject("user_id", UUID.class),
						resultSet.getString("display_name"),
						resultSet.getString("home_city"),
						resultSet.getString("preferred_locale")),
				userId);
	}

	private void lockActiveUser(UUID userId) {
		List<UUID> users = jdbcTemplate.query(
				"SELECT id FROM users WHERE id = ? AND status = 'ACTIVE' FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				userId);
		if (users.isEmpty()) {
			throw new IllegalArgumentException("Unknown active user: " + userId);
		}
	}

	private void requireUserId(UUID userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}
	}

	private String optionalText(String value, int maxLength, String fieldName) {
		if (value == null) {
			return null;
		}
		String clean = value.strip();
		if (clean.isEmpty()) {
			return null;
		}
		if (clean.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " is too long");
		}
		return clean;
	}

	private String requiredText(String value, int maxLength, String fieldName) {
		String clean = optionalText(value, maxLength, fieldName);
		if (clean == null) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return clean;
	}
}
