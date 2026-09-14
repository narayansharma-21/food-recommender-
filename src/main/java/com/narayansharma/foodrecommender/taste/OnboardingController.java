package com.narayansharma.foodrecommender.taste;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PutMapping("/{onboardingDishId}")
	public OnboardingResponseView saveResponse(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID onboardingDishId,
			@Valid @RequestBody SaveOnboardingResponseRequest request) {
		return onboardingService.saveResponse(principal.userId(), onboardingDishId, request);
	}
}
