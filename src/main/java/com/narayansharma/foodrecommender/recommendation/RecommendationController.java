package com.narayansharma.foodrecommender.recommendation;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/restaurants/{restaurantId}/recommendations")
public class RecommendationController {
	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@PostMapping
	public RecommendationResponse recommend(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID restaurantId,
			@Valid @RequestBody RecommendationRequest request) {
		return recommendationService.recommend(principal.userId(), restaurantId, request);
	}
}
