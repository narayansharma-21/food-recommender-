package com.narayansharma.foodrecommender.taste;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TasteProfileSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesVersionedFeaturesWithTraceableEvidence() {
		UUID userId = UUID.randomUUID();
		UUID featureId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		jdbcTemplate.update("""
				INSERT INTO taste_profiles (user_id, calculation_version, calculated_at)
				VALUES (?, 'weighted-v1', CURRENT_TIMESTAMP)
				""", userId);
		jdbcTemplate.update("""
				INSERT INTO taste_profile_features (
				    id, user_id, feature_type, feature_key, display_name,
				    preference_score, evidence_count, calculation_version, updated_at
				) VALUES (?, ?, 'TRAIT', 'crispy', 'Crispy', ?, 1, 'weighted-v1', CURRENT_TIMESTAMP)
				""", featureId, userId, new BigDecimal("0.500000"));
		jdbcTemplate.update("""
				INSERT INTO taste_profile_evidence (
				    feature_id, source_type, source_id, contribution, created_at
				) VALUES (?, 'RATING_REVISION', ?, ?, CURRENT_TIMESTAMP)
				""", featureId, sourceId, new BigDecimal("1.000000"));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT evidence_count FROM taste_profile_features WHERE id = ?", Integer.class, featureId))
				.isEqualTo(1);
	}
}
