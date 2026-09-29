package com.narayansharma.foodrecommender.catalog.selection;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCandidate;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import com.narayansharma.foodrecommender.catalog.duplicates.RestaurantDuplicateReviewService;
import com.narayansharma.foodrecommender.catalog.identifiers.ResolvedRestaurantLocation;
import com.narayansharma.foodrecommender.catalog.identifiers.RestaurantExternalIdService;
import com.narayansharma.foodrecommender.catalog.matching.KnownRestaurantLocation;
import com.narayansharma.foodrecommender.catalog.matching.RestaurantMatchLevel;
import com.narayansharma.foodrecommender.catalog.matching.RestaurantMatchResult;
import com.narayansharma.foodrecommender.catalog.matching.RestaurantMatcher;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantSelectionService {
	private final JdbcTemplate jdbcTemplate;
	private final RestaurantExternalIdService externalIdService;
	private final RestaurantMatcher matcher;
	private final RestaurantDuplicateReviewService duplicateReviewService;
	private final Clock clock;
	private final String defaultRegion;
	private final String defaultCountryCode;

	public RestaurantSelectionService(
			JdbcTemplate jdbcTemplate,
			RestaurantExternalIdService externalIdService,
			RestaurantMatcher matcher,
			RestaurantDuplicateReviewService duplicateReviewService,
			Clock clock,
			@Value("${catalog.launch-area.region:MA}") String defaultRegion,
			@Value("${catalog.launch-area.country-code:US}") String defaultCountryCode) {
		this.jdbcTemplate = jdbcTemplate;
		this.externalIdService = externalIdService;
		this.matcher = matcher;
		this.duplicateReviewService = duplicateReviewService;
		this.clock = clock;
		this.defaultRegion = defaultRegion;
		this.defaultCountryCode = defaultCountryCode;
	}

	@Transactional
	public SelectedRestaurant select(ExternalRestaurantId externalId) {
		if (externalId == null) {
			throw new IllegalArgumentException("External restaurant ID is required");
		}
		SourceRestaurant source = lockSource(externalId);
		ResolvedRestaurantLocation existing = externalIdService.resolve(externalId).orElse(null);
		if (existing != null) {
			return new SelectedRestaurant(
					existing.restaurantId(),
					existing.locationId(),
					false,
					RestaurantMatchLevel.EXACT,
					false);
		}

		RestaurantCandidate candidate = source.candidate(externalId, defaultRegion, defaultCountryCode);
		RestaurantMatchResult match = matcher.match(candidate, knownLocations(candidate.address()));
		if (match.level() == RestaurantMatchLevel.LIKELY || match.level() == RestaurantMatchLevel.EXACT) {
			externalIdService.attach(externalId, match.location().locationId());
			return new SelectedRestaurant(
					match.location().restaurantId(),
					match.location().locationId(),
					false,
					match.level(),
					false);
		}

		CreatedRestaurant created = create(candidate);
		externalIdService.attach(externalId, created.locationId());
		if (match.level() == RestaurantMatchLevel.REVIEW) {
			duplicateReviewService.suggest(
					match.location().locationId(),
					created.locationId(),
					match.score(),
					match.reasons());
		}
		return new SelectedRestaurant(
				created.restaurantId(),
				created.locationId(),
				true,
				match.level(),
				match.level() == RestaurantMatchLevel.REVIEW);
	}

	private SourceRestaurant lockSource(ExternalRestaurantId externalId) {
		return jdbcTemplate.query("""
				SELECT display_name, address_line_1, address_line_2,
				       city, region, postal_code, country_code, latitude, longitude,
				       phone, website_url
				FROM restaurant_source_records
				WHERE source = ? AND external_id = ?
				FOR UPDATE
				""", (resultSet, rowNumber) -> new SourceRestaurant(
				resultSet.getString("display_name"),
				resultSet.getString("address_line_1"),
				resultSet.getString("address_line_2"),
				resultSet.getString("city"),
				resultSet.getString("region"),
				resultSet.getString("postal_code"),
				resultSet.getString("country_code"),
				resultSet.getBigDecimal("latitude"),
				resultSet.getBigDecimal("longitude"),
				resultSet.getString("phone"),
				resultSet.getString("website_url")), externalId.source(), externalId.value()).stream()
				.findFirst()
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"RESTAURANT_SOURCE_NOT_FOUND",
						"The restaurant search result was not found."));
	}

	private List<KnownRestaurantLocation> knownLocations(RestaurantAddress address) {
		return jdbcTemplate.query("""
				SELECT restaurant.id AS restaurant_id, location.id AS location_id,
				       restaurant.display_name, location.address_line_1, location.address_line_2,
				       location.city, location.region, location.postal_code, location.country_code,
				       location.latitude, location.longitude, location.phone_e164
				FROM restaurant_locations location
				JOIN restaurants restaurant ON restaurant.id = location.restaurant_id
				WHERE location.status = 'ACTIVE' AND location.merged_into_id IS NULL
				  AND restaurant.merged_into_id IS NULL
				  AND LOWER(location.city) = LOWER(?)
				  AND LOWER(location.region) = LOWER(?)
				ORDER BY location.id
				""", (resultSet, rowNumber) -> {
			BigDecimal latitude = resultSet.getBigDecimal("latitude");
			BigDecimal longitude = resultSet.getBigDecimal("longitude");
			return new KnownRestaurantLocation(
					resultSet.getObject("restaurant_id", UUID.class),
					resultSet.getObject("location_id", UUID.class),
					resultSet.getString("display_name"),
					new RestaurantAddress(
							resultSet.getString("address_line_1"),
							resultSet.getString("address_line_2"),
							resultSet.getString("city"),
							resultSet.getString("region"),
							resultSet.getString("postal_code"),
							resultSet.getString("country_code")),
					latitude == null || longitude == null
							? null
							: new RestaurantCoordinates(latitude, longitude),
					resultSet.getString("phone_e164"),
					List.of());
		}, address.city(), address.region());
	}

	private CreatedRestaurant create(RestaurantCandidate candidate) {
		RestaurantAddress address = candidate.address();
		if (address.addressLine1() == null || address.addressLine1().isBlank()
				|| address.city() == null || address.city().isBlank()
				|| address.region() == null || address.region().isBlank()
				|| address.countryCode() == null || !address.countryCode().matches("[A-Z]{2}")) {
			throw new ApiException(
					HttpStatus.UNPROCESSABLE_CONTENT,
					"RESTAURANT_ADDRESS_REQUIRED",
					"The restaurant needs a complete address before it can be selected.");
		}
		UUID restaurantId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		Instant now = clock.instant();
		jdbcTemplate.update("""
				INSERT INTO restaurants (
				    id, display_name, normalized_name, website_url, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				restaurantId,
				candidate.displayName(),
				com.narayansharma.foodrecommender.catalog.discovery.RestaurantTextNormalizer.normalize(
						candidate.displayName()),
				validWebsite(candidate.website()),
				Timestamp.from(now),
				Timestamp.from(now));
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, address_line_2, city, region,
				    postal_code, country_code, latitude, longitude, phone_e164,
				    timezone, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'America/New_York', ?, ?)
				""",
				locationId,
				restaurantId,
				address.addressLine1(),
				address.addressLine2(),
				address.city(),
				address.region(),
				address.postalCode(),
				address.countryCode(),
				candidate.coordinates() == null ? null : candidate.coordinates().latitude(),
				candidate.coordinates() == null ? null : candidate.coordinates().longitude(),
				validE164(candidate.phone()),
				Timestamp.from(now),
				Timestamp.from(now));
		return new CreatedRestaurant(restaurantId, locationId);
	}

	private String validWebsite(URI website) {
		return website == null ? null : website.toString();
	}

	private String validE164(String phone) {
		return phone != null && phone.matches("\\+[1-9][0-9]{6,14}") ? phone : null;
	}

	private record SourceRestaurant(
			String displayName,
			String addressLine1,
			String addressLine2,
			String city,
			String region,
			String postalCode,
			String countryCode,
			BigDecimal latitude,
			BigDecimal longitude,
			String phone,
			String website) {
		RestaurantCandidate candidate(
				ExternalRestaurantId externalId,
				String defaultRegion,
				String defaultCountryCode) {
			RestaurantAddress address = new RestaurantAddress(
					addressLine1,
					addressLine2,
					city,
					normalizeRegion(region, defaultRegion),
					postalCode,
					countryCode == null ? defaultCountryCode : countryCode.toUpperCase(Locale.ROOT));
			RestaurantCoordinates coordinates = latitude == null || longitude == null
					? null
					: new RestaurantCoordinates(latitude, longitude);
			return new RestaurantCandidate(
					externalId,
					displayName,
					address,
					coordinates,
					phone,
					parseWebsite(website));
		}

		private URI parseWebsite(String value) {
			if (value == null || value.isBlank()) {
				return null;
			}
			try {
				URI uri = URI.create(value);
				return uri.isAbsolute() && ("http".equalsIgnoreCase(uri.getScheme())
						|| "https".equalsIgnoreCase(uri.getScheme())) ? uri : null;
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}

		private String normalizeRegion(String value, String defaultValue) {
			if (value == null || ("MA".equalsIgnoreCase(defaultValue)
					&& "Massachusetts".equalsIgnoreCase(value))) {
				return defaultValue;
			}
			return value;
		}
	}

	private record CreatedRestaurant(UUID restaurantId, UUID locationId) {
	}
}
