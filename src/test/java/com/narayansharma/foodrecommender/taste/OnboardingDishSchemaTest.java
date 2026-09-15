package com.narayansharma.foodrecommender.taste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OnboardingDishSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private OnboardingService onboardingService;

	@Test
	void seedsACompactGreaterBostonQuestionSet() {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM onboarding_dishes
				WHERE launch_city = 'Greater Boston' AND active = TRUE
				""", Integer.class);

		assertThat(count).isEqualTo(12);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM onboarding_dishes onboarding
				JOIN dish_concept_cuisines mapping ON mapping.dish_concept_id = onboarding.dish_concept_id
				WHERE onboarding.launch_city = 'Greater Boston'
				""", Integer.class)).isEqualTo(12);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM onboarding_dishes onboarding
				JOIN dish_concept_ingredients mapping ON mapping.dish_concept_id = onboarding.dish_concept_id
				WHERE onboarding.launch_city = 'Greater Boston'
				""", Integer.class)).isEqualTo(12);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM onboarding_dishes onboarding
				JOIN dish_concept_preparations mapping ON mapping.dish_concept_id = onboarding.dish_concept_id
				WHERE onboarding.launch_city = 'Greater Boston'
				""", Integer.class)).isEqualTo(12);
	}

	@Test
	void returnsTheOrderedQuestionSetToAnActiveUser() {
		UUID userId = insertUser();

		assertThat(onboardingService.unansweredQuestions(userId))
				.hasSize(12)
				.first()
				.extracting(OnboardingDishQuestion::dishName)
				.isEqualTo("Lobster Roll");
	}

	@Test
	void savesAndUpdatesAUsersOnboardingAnswer() {
		UUID userId = insertUser();
		UUID dishId = onboardingService.unansweredQuestions(userId).getFirst().onboardingDishId();

		OnboardingResponseView first = onboardingService.saveResponse(
				userId, dishId, new SaveOnboardingResponseRequest(4));
		OnboardingResponseView updated = onboardingService.saveResponse(
				userId, dishId, new SaveOnboardingResponseRequest(5));

		assertThat(updated.id()).isEqualTo(first.id());
		assertThat(updated.preferenceScore()).isEqualTo(5);
		assertThat(onboardingService.unansweredQuestions(userId)).hasSize(11);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM taste_profile_features WHERE user_id = ?",
				Integer.class,
				userId)).isEqualTo(3);
	}

	private UUID insertUser() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);
		return userId;
	}
}
