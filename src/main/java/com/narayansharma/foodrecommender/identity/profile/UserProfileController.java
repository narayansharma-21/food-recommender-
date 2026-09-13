package com.narayansharma.foodrecommender.identity.profile;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users/me")
public class UserProfileController {
	private final UserProfileService profileService;

	public UserProfileController(UserProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	public UserProfile get(@AuthenticationPrincipal UserPrincipal principal) {
		return profileService.get(principal.userId());
	}

	@PatchMapping
	public UserProfile update(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody UpdateUserProfileRequest request) {
		return profileService.update(
				principal.userId(),
				request.displayName(),
				request.homeCity(),
				request.preferredLocale());
	}
}
