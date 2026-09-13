package com.narayansharma.foodrecommender.identity;

import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
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
public class UserProvisioningService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final String defaultHomeCity;
	private final String defaultLocale;

	public UserProvisioningService(
			JdbcTemplate jdbcTemplate,
			Clock clock,
			@Value("${identity.profile.default-home-city:Greater Boston}") String defaultHomeCity,
			@Value("${identity.profile.default-locale:en-US}") String defaultLocale) {
		if (defaultHomeCity == null
				|| defaultHomeCity.isBlank()
				|| defaultHomeCity.length() > 100
				|| defaultLocale == null
				|| defaultLocale.isBlank()
				|| defaultLocale.length() > 20) {
			throw new IllegalArgumentException("Default user profile values are invalid");
		}
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.defaultHomeCity = defaultHomeCity;
		this.defaultLocale = defaultLocale;
	}

	@Transactional
	public InternalUser findOrCreate(VerifiedIdentity identity) {
		if (identity == null) {
			throw new IllegalArgumentException("Verified identity is required");
		}
		List<UserRow> existing = find(identity);
		if (!existing.isEmpty()) {
			return active(existing.getFirst(), false);
		}
		lockProvisioningBucket(identity);
		existing = find(identity);
		if (!existing.isEmpty()) {
			return active(existing.getFirst(), false);
		}

		UUID userId = UUID.randomUUID();
		Instant now = clock.instant();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', ?, ?)
				""", userId, Timestamp.from(now), Timestamp.from(now));
		jdbcTemplate.update("""
				INSERT INTO user_auth_identities (
				    id, user_id, provider, provider_subject, created_at
				) VALUES (?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				userId,
				identity.provider(),
				identity.subject(),
				Timestamp.from(now));
		jdbcTemplate.update("""
				INSERT INTO user_profiles (
				    user_id, display_name, home_city, preferred_locale, created_at, updated_at
				) VALUES (?, NULL, ?, ?, ?, ?)
				""",
				userId,
				defaultHomeCity,
				defaultLocale,
				Timestamp.from(now),
				Timestamp.from(now));
		return new InternalUser(userId, true);
	}

	private void lockProvisioningBucket(VerifiedIdentity identity) {
		int identityHash = 31 * identity.provider().hashCode() + identity.subject().hashCode();
		int lockKey = Math.floorMod(identityHash, 16);
		jdbcTemplate.queryForObject(
				"SELECT lock_key FROM identity_provisioning_locks WHERE lock_key = ? FOR UPDATE",
				Integer.class,
				lockKey);
	}

	private List<UserRow> find(VerifiedIdentity identity) {
		return jdbcTemplate.query("""
				SELECT users.id, users.status
				FROM user_auth_identities identity
				JOIN users ON users.id = identity.user_id
				WHERE identity.provider = ? AND identity.provider_subject = ?
				""",
				(resultSet, rowNumber) -> new UserRow(
						resultSet.getObject("id", UUID.class),
						resultSet.getString("status")),
				identity.provider(),
				identity.subject());
	}

	private InternalUser active(UserRow user, boolean created) {
		if (!"ACTIVE".equals(user.status())) {
			throw new IllegalStateException("User account is not active");
		}
		return new InternalUser(user.id(), created);
	}

	private record UserRow(UUID id, String status) {
	}
}
