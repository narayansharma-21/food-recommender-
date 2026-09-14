package com.narayansharma.foodrecommender.taste;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TasteProfileQueryService {
	private final JdbcTemplate jdbcTemplate;

	public TasteProfileQueryService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public TasteProfileView get(UUID userId) {
		requireActiveUser(userId);
		List<TasteProfileView> profiles = jdbcTemplate.query("""
				SELECT calculation_version, calculated_at
				FROM taste_profiles
				WHERE user_id = ?
				""", (resultSet, rowNumber) -> new TasteProfileView(
				resultSet.getString("calculation_version"),
				resultSet.getTimestamp("calculated_at").toInstant(),
				List.of()), userId);
		if (profiles.isEmpty()) {
			return new TasteProfileView(null, null, List.of());
		}
		TasteProfileView profile = profiles.getFirst();
		return new TasteProfileView(
				profile.calculationVersion(),
				profile.calculatedAt(),
				features(userId));
	}

	private List<TasteFeatureView> features(UUID userId) {
		List<TasteFeatureView> features = jdbcTemplate.query("""
				SELECT id, feature_type, feature_key, display_name,
				       preference_score, evidence_count
				FROM taste_profile_features
				WHERE user_id = ?
				ORDER BY feature_type, preference_score DESC, feature_key
				""", (resultSet, rowNumber) -> new TasteFeatureView(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("feature_type"),
				resultSet.getString("feature_key"),
				resultSet.getString("display_name"),
				resultSet.getBigDecimal("preference_score"),
				resultSet.getInt("evidence_count"),
				List.of()), userId);
		return features.stream()
				.map(feature -> new TasteFeatureView(
						feature.id(),
						feature.type(),
						feature.key(),
						feature.displayName(),
						feature.preferenceScore(),
						feature.evidenceCount(),
						evidence(feature.id())))
				.toList();
	}

	private List<TasteEvidenceView> evidence(UUID featureId) {
		return jdbcTemplate.query("""
				SELECT source_type, source_id, contribution
				FROM taste_profile_evidence
				WHERE feature_id = ?
				ORDER BY source_type, source_id
				""", (resultSet, rowNumber) -> new TasteEvidenceView(
				resultSet.getString("source_type"),
				resultSet.getObject("source_id", UUID.class),
				resultSet.getBigDecimal("contribution")), featureId);
	}

	private void requireActiveUser(UUID userId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE id = ? AND status = 'ACTIVE'",
				Integer.class,
				userId);
		if (count == null || count == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "The active user was not found.");
		}
	}
}
