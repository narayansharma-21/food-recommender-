package com.narayansharma.foodrecommender.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserPreferenceSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesCurrentRestrictionsAndAppendOnlyConsentHistory() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		jdbcTemplate.update("""
				INSERT INTO user_restrictions (
				    id, user_id, restriction_type, restriction_key,
				    display_name, active, created_at, updated_at
				) VALUES (?, ?, 'ALLERGY', 'shellfish', 'Shellfish', TRUE,
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), userId);
		jdbcTemplate.update("""
				INSERT INTO user_consent_events (
				    id, user_id, consent_type, event_sequence,
				    granted, policy_version, recorded_at
				) VALUES (?, ?, 'LOCATION_DATA', 1, TRUE, 'privacy-v1', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), userId);
		jdbcTemplate.update("""
				INSERT INTO user_consent_events (
				    id, user_id, consent_type, event_sequence,
				    granted, policy_version, recorded_at
				) VALUES (?, ?, 'LOCATION_DATA', 2, FALSE, 'privacy-v1', CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), userId);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM user_restrictions WHERE user_id = ? AND active = TRUE",
				Integer.class,
				userId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM user_consent_events WHERE user_id = ?",
				Integer.class,
				userId)).isEqualTo(2);
	}
}
