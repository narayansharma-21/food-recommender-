package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/menus")
public class AdminOfficialMenuController {
	private final OfficialMenuSubmissionService submissionService;

	public AdminOfficialMenuController(OfficialMenuSubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@PostMapping("/official-sources")
	public ResponseEntity<OfficialMenuSubmissionResponse> submit(
			@Valid @RequestBody OfficialMenuSubmissionRequest request) {
		try {
			OfficialMenuSubmissionResponse response = submissionService.submit(
					request.locationId(),
					request.menuKey(),
					request.displayName(),
					request.sourceType(),
					request.url());
			return ResponseEntity.accepted().body(response);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"INVALID_OFFICIAL_MENU_SOURCE",
					exception.getMessage());
		}
	}
}
