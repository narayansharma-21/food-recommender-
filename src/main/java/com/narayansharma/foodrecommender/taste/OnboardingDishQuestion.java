package com.narayansharma.foodrecommender.taste;

import java.util.UUID;

public record OnboardingDishQuestion(
		UUID onboardingDishId,
		UUID dishConceptId,
		String dishName,
		String prompt) {
}
