package com.narayansharma.foodrecommender.administration;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/admin/jobs")
public class AdminJobController {
	private final AdminJobService jobService;

	public AdminJobController(AdminJobService jobService) {
		this.jobService = jobService;
	}

	@GetMapping("/failed")
	public List<FailedJobView> failed(
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
		return jobService.failedJobs(limit);
	}

	@PostMapping("/{jobId}/retry")
	public ResponseEntity<Void> retry(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID jobId,
			@Valid @RequestBody RetryFailedJobRequest request) {
		jobService.retry(jobId, principal.userId(), request.reason());
		return ResponseEntity.accepted().build();
	}
}
