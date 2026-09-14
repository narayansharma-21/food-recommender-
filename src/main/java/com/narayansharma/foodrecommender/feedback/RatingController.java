package com.narayansharma.foodrecommender.feedback;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ratings")
public class RatingController {
	private final RatingService ratingService;
	private final RatingQueryService ratingQueryService;
	private final RatingModerationService moderationService;

	public RatingController(
			RatingService ratingService,
			RatingQueryService ratingQueryService,
			RatingModerationService moderationService) {
		this.ratingService = ratingService;
		this.ratingQueryService = ratingQueryService;
		this.moderationService = moderationService;
	}

	@PostMapping
	public ResponseEntity<RatingView> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody SaveRatingRequest request) {
		UUID ratingId = ratingService.create(principal.userId(), request);
		return ResponseEntity.created(URI.create("/v1/ratings/" + ratingId))
				.body(ratingQueryService.get(principal.userId(), ratingId));
	}

	@GetMapping("/{ratingId}")
	public RatingView get(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID ratingId) {
		return ratingQueryService.get(principal.userId(), ratingId);
	}

	@PutMapping("/{ratingId}")
	public RatingView update(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID ratingId,
			@Valid @RequestBody SaveRatingRequest request) {
		ratingService.update(principal.userId(), ratingId, request);
		return ratingQueryService.get(principal.userId(), ratingId);
	}

	@DeleteMapping("/{ratingId}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID ratingId) {
		ratingService.delete(principal.userId(), ratingId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{ratingId}/history")
	public List<RatingRevisionView> history(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID ratingId) {
		return ratingQueryService.history(principal.userId(), ratingId);
	}

	@PostMapping("/{ratingId}/reports")
	public ResponseEntity<ReportRatingResponse> report(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID ratingId,
			@Valid @RequestBody ReportRatingRequest request) {
		return ResponseEntity.accepted().body(moderationService.report(principal.userId(), ratingId, request));
	}
}
