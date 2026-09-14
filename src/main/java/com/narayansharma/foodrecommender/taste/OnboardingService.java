package com.narayansharma.foodrecommender.taste;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {
	static final String LAUNCH_CITY = "Greater Boston";
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public OnboardingService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public OnboardingResponseView saveResponse(
			UUID userId,
			UUID onboardingDishId,
			SaveOnboardingResponseRequest request) {
		if (request == null || request.preferenceScore() < 1 || request.preferenceScore() > 5) {
			throw new IllegalArgumentException("Preference score must be between 1 and 5");
		}
		lockActiveUser(userId);
		requireActiveOnboardingDish(onboardingDishId);
		Instant now = clock.instant();
		List<UUID> existingIds = jdbcTemplate.query(
				"SELECT id FROM onboarding_responses WHERE user_id = ? AND onboarding_dish_id = ?",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				userId,
				onboardingDishId);
		UUID responseId = existingIds.isEmpty() ? UUID.randomUUID() : existingIds.getFirst();
		if (existingIds.isEmpty()) {
			jdbcTemplate.update("""
					INSERT INTO onboarding_responses (
					    id, user_id, onboarding_dish_id, preference_score, created_at, updated_at
					) VALUES (?, ?, ?, ?, ?, ?)
					""", responseId, userId, onboardingDishId, request.preferenceScore(),
					Timestamp.from(now), Timestamp.from(now));
		} else {
			jdbcTemplate.update("""
					UPDATE onboarding_responses
					SET preference_score = ?, updated_at = ?
					WHERE id = ?
					""", request.preferenceScore(), Timestamp.from(now), responseId);
		}
		return new OnboardingResponseView(responseId, onboardingDishId, request.preferenceScore(), now);
	}

	public List<OnboardingDishQuestion> unansweredQuestions(UUID userId) {
		requireActiveUser(userId);
		return jdbcTemplate.query("""
				SELECT onboarding.id, onboarding.dish_concept_id, dish.display_name, onboarding.prompt
				FROM onboarding_dishes onboarding
				JOIN dish_concepts dish ON dish.id = onboarding.dish_concept_id
				LEFT JOIN onboarding_responses response
				    ON response.onboarding_dish_id = onboarding.id AND response.user_id = ?
				WHERE onboarding.launch_city = ?
				  AND onboarding.active = TRUE
				  AND response.id IS NULL
				ORDER BY onboarding.display_order
				LIMIT 15
				""", (resultSet, rowNumber) -> new OnboardingDishQuestion(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("dish_concept_id", UUID.class),
				resultSet.getString("display_name"),
				resultSet.getString("prompt")), userId, LAUNCH_CITY);
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

	private void lockActiveUser(UUID userId) {
		List<UUID> users = jdbcTemplate.query(
				"SELECT id FROM users WHERE id = ? AND status = 'ACTIVE' FOR UPDATE",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				userId);
		if (users.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "The active user was not found.");
		}
	}

	private void requireActiveOnboardingDish(UUID onboardingDishId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM onboarding_dishes
				WHERE id = ? AND launch_city = ? AND active = TRUE
				""", Integer.class, onboardingDishId, LAUNCH_CITY);
		if (count == null || count == 0) {
			throw new ApiException(
					HttpStatus.NOT_FOUND,
					"ONBOARDING_DISH_NOT_FOUND",
					"The onboarding dish was not found.");
		}
	}
}
