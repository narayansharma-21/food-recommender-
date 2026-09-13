package com.narayansharma.foodrecommender.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserIdentitySchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void keepsInternalUsersSeparateFromProviderSubjects() {
		UUID userId = UUID.randomUUID();
		insertUser(userId);
		jdbcTemplate.update("""
				INSERT INTO user_auth_identities (
				    id, user_id, provider, provider_subject, created_at
				) VALUES (?, ?, 'firebase', 'firebase-user-123', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), userId);
		jdbcTemplate.update("""
				INSERT INTO user_profiles (
				    user_id, display_name, home_city, preferred_locale, created_at, updated_at
				) VALUES (?, 'Sam', 'Greater Boston', 'en-US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT user_id FROM user_auth_identities WHERE provider_subject = 'firebase-user-123'",
				UUID.class)).isEqualTo(userId);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT home_city FROM user_profiles WHERE user_id = ?",
				String.class,
				userId)).isEqualTo("Greater Boston");
	}

	@Test
	void preventsOneProviderSubjectFromOwningMultipleUsers() {
		UUID firstUserId = UUID.randomUUID();
		UUID secondUserId = UUID.randomUUID();
		insertUser(firstUserId);
		insertUser(secondUserId);
		jdbcTemplate.update("""
				INSERT INTO user_auth_identities (
				    id, user_id, provider, provider_subject, created_at
				) VALUES (?, ?, 'firebase', 'same-subject', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), firstUserId);

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO user_auth_identities (
				    id, user_id, provider, provider_subject, created_at
				) VALUES (?, ?, 'firebase', 'same-subject', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), secondUserId))
				.isInstanceOf(DuplicateKeyException.class);
	}

	private void insertUser(UUID userId) {
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
	}
}
