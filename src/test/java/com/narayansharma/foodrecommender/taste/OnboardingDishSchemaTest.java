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
	}

	@Test
	void returnsTheOrderedQuestionSetToAnActiveUser() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO users (id, status, created_at, updated_at)
				VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", userId);

		assertThat(onboardingService.unansweredQuestions(userId))
				.hasSize(12)
				.first()
				.extracting(OnboardingDishQuestion::dishName)
				.isEqualTo("Lobster Roll");
	}
}
