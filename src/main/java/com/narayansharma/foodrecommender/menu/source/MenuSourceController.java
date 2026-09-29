package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.identity.auth.UserPrincipal;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/menus")
public class MenuSourceController {
	private final MenuUploadService uploadService;
	private final MenuUploadRateLimiter rateLimiter;

	public MenuSourceController(
			MenuUploadService uploadService,
			MenuUploadRateLimiter rateLimiter) {
		this.uploadService = uploadService;
		this.rateLimiter = rateLimiter;
	}

	@PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MenuUploadResponse> upload(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam UUID locationId,
			@RequestParam(defaultValue = "main") String menuKey,
			@RequestParam(defaultValue = "Main Menu") String displayName,
			@RequestPart("file") MultipartFile file) {
		rateLimiter.requireAllowed(principal.userId());
		if (file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MENU_UPLOAD", "Menu image content is required.");
		}
		try (InputStream content = file.getInputStream()) {
			MenuUploadResponse response = uploadService.upload(
					principal.userId(),
					locationId,
					menuKey,
					displayName,
					file.getContentType(),
					content);
			return ResponseEntity.accepted().body(response);
		} catch (MenuUploadTooLargeException exception) {
			throw new ApiException(
					HttpStatus.CONTENT_TOO_LARGE,
					"MENU_UPLOAD_TOO_LARGE",
					exception.getMessage());
		} catch (IllegalArgumentException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MENU_UPLOAD", exception.getMessage());
		} catch (IOException exception) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"MENU_UPLOAD_READ_FAILED",
					"The menu image could not be read.");
		}
	}
}
