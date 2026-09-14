package com.narayansharma.foodrecommender.taste;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OnboardingService {
	static final String LAUNCH_CITY = "Greater Boston";
	private final JdbcTemplate jdbcTemplate;

	public OnboardingService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
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
}
