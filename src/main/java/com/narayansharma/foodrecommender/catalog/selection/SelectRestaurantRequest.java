package com.narayansharma.foodrecommender.catalog.selection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SelectRestaurantRequest(
		@NotBlank @Pattern(regexp = "[a-z][a-z0-9_-]{0,49}") String source,
		@NotBlank @Size(max = 255) String externalId) {
}
