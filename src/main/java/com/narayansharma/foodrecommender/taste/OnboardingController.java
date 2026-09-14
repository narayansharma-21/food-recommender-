package com.narayansharma.foodrecommender.taste;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/onboarding/dishes")
public class OnboardingController {
	private final OnboardingService onboardingService;

	public OnboardingController(OnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	@GetMapping
	public List<OnboardingDishQuestion> questions(
			@AuthenticationPrincipal UserPrincipal principal) {
		return onboardingService.unansweredQuestions(principal.userId());
	}
}
