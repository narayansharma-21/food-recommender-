package com.narayansharma.foodrecommender.catalog.query;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchQuery;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/restaurants")
public class RestaurantQueryController {
	private final RestaurantSearchService searchService;
	private final RestaurantQueryService queryService;

	public RestaurantQueryController(
			RestaurantSearchService searchService,
			RestaurantQueryService queryService) {
		this.searchService = searchService;
		this.queryService = queryService;
	}

	@GetMapping("/{restaurantId}")
	public RestaurantView get(@PathVariable UUID restaurantId) {
		return queryService.get(restaurantId);
	}

	@GetMapping("/search")
	public RestaurantSearchResponse search(
			@RequestParam("q") @NotBlank @Size(max = 100) String text,
			@RequestParam(defaultValue = "Greater Boston") @NotBlank @Size(max = 100) String city,
			@RequestParam(defaultValue = "MA") @Pattern(regexp = "[A-Z]{2}") String region,
			@RequestParam(defaultValue = "US") @Pattern(regexp = "[A-Z]{2}") String countryCode,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
			@RequestParam(required = false) @Size(max = 100) String cursor) {
		try {
			return searchService.search(new RestaurantSearchQuery(
					text, city, region, countryCode, limit, cursor));
		} catch (IllegalArgumentException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESTAURANT_SEARCH", exception.getMessage());
		}
	}
}
