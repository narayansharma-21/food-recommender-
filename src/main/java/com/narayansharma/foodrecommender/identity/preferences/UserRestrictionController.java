package com.narayansharma.foodrecommender.identity.preferences;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users/me/restrictions")
@Validated
public class UserRestrictionController {
	private final UserRestrictionService restrictionService;

	public UserRestrictionController(UserRestrictionService restrictionService) {
		this.restrictionService = restrictionService;
	}

	@GetMapping
	public List<UserRestriction> active(@AuthenticationPrincipal UserPrincipal principal) {
		return restrictionService.active(principal.userId());
	}

	@PutMapping("/{type}/{key}")
	public UserRestriction set(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable RestrictionType type,
			@PathVariable @Size(max = 100) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*") String key,
			@Valid @RequestBody SetUserRestrictionRequest request) {
		return restrictionService.set(
				principal.userId(),
				type,
				key,
				request.displayName(),
				request.active());
	}
}
