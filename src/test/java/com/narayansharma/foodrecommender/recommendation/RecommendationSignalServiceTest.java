package com.narayansharma.foodrecommender.recommendation;

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
class RecommendationSignalServiceTest {
	@Autowired
	private RecommendationSignalService signalService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void combinesMatchingTasteFeaturesWithAColdStartPrior() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		UUID cuisineFeatureId = UUID.randomUUID();
		UUID preparationFeatureId = UUID.randomUUID();
		UUID firstSourceId = UUID.randomUUID();
		UUID secondSourceId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO taste_profile_features (
				    id, user_id, feature_type, feature_key, display_name,
				    preference_score, evidence_count, calculation_version, updated_at
				) VALUES (?, ?, 'CUISINE', 'american', 'American',
				          0.5, 2, 'test-v1', CURRENT_TIMESTAMP)
				""", cuisineFeatureId, userId);
		jdbcTemplate.update("""
				INSERT INTO taste_profile_features (
				    id, user_id, feature_type, feature_key, display_name,
				    preference_score, evidence_count, calculation_version, updated_at
				) VALUES (?, ?, 'PREPARATION', 'sandwich', 'Sandwich',
				          0.5, 2, 'test-v1', CURRENT_TIMESTAMP)
				""", preparationFeatureId, userId);
		insertEvidence(cuisineFeatureId, firstSourceId);
		insertEvidence(cuisineFeatureId, secondSourceId);
		insertEvidence(preparationFeatureId, firstSourceId);
		insertEvidence(preparationFeatureId, secondSourceId);
		RecommendationCandidate candidate = new RecommendationCandidate(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.fromString("10000000-0000-0000-0000-000000000004"),
				"Cheeseburger");

		RecommendationSignals signals = signalService.signals(userId, candidate);

		assertThat(signals.personalPreference()).isEqualByComparingTo(new BigDecimal("0.500000"));
		assertThat(signals.popularity()).isEqualByComparingTo(new BigDecimal("0.625000"));
		assertThat(signals.evidenceCount()).isEqualTo(2);
		assertThat(signals.popularityRatingCount()).isZero();
		assertThat(signals.previouslyRated()).isFalse();
	}

	private void insertEvidence(UUID featureId, UUID sourceId) {
		jdbcTemplate.update("""
				INSERT INTO taste_profile_evidence (
				    feature_id, source_type, source_id, contribution, created_at
				) VALUES (?, 'ONBOARDING_RESPONSE', ?, 0.5, CURRENT_TIMESTAMP)
				""", featureId, sourceId);
	}
}
