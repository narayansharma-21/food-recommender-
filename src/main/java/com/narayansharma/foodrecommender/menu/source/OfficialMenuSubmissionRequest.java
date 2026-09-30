package com.narayansharma.foodrecommender.menu.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;

public record OfficialMenuSubmissionRequest(
		@NotNull UUID locationId,
		@NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9_-]{0,49}") String menuKey,
		@NotBlank @Size(max = 100) String displayName,
		@NotNull MenuSourceType sourceType,
		@NotNull URI url) {
}
