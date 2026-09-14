package com.narayansharma.foodrecommender.taste;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users/me/taste-profile")
public class TasteProfileController {
	private final TasteProfileQueryService queryService;

	public TasteProfileController(TasteProfileQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping
	public TasteProfileView get(@AuthenticationPrincipal UserPrincipal principal) {
		return queryService.get(principal.userId());
	}
}
