package com.narayansharma.foodrecommender.catalog.query;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantQueryService {
	private final JdbcTemplate jdbcTemplate;

	public RestaurantQueryService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public RestaurantView get(UUID restaurantId) {
		if (restaurantId == null) {
			throw new IllegalArgumentException("Restaurant ID is required");
		}
		RestaurantSummary restaurant = jdbcTemplate.query("""
				SELECT canonical.id, canonical.display_name, canonical.website_url
				FROM restaurants requested
				JOIN restaurants canonical
				  ON canonical.id = COALESCE(requested.merged_into_id, requested.id)
				WHERE requested.id = ?
				""", (resultSet, rowNumber) -> new RestaurantSummary(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("display_name"),
				website(resultSet.getString("website_url"))), restaurantId).stream()
				.findFirst()
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"RESTAURANT_NOT_FOUND",
						"The restaurant was not found."));

		List<RestaurantLocationView> locations = jdbcTemplate.query("""
				SELECT id, address_line_1, address_line_2, city, region, postal_code,
				       country_code, latitude, longitude, phone_e164, timezone
				FROM restaurant_locations
				WHERE restaurant_id = ? AND status = 'ACTIVE' AND merged_into_id IS NULL
				ORDER BY city, address_line_1, id
				""", (resultSet, rowNumber) -> {
			BigDecimal latitude = resultSet.getBigDecimal("latitude");
			BigDecimal longitude = resultSet.getBigDecimal("longitude");
			return new RestaurantLocationView(
					resultSet.getObject("id", UUID.class),
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
					resultSet.getString("timezone"));
		}, restaurant.id());
		return new RestaurantView(restaurant.id(), restaurant.displayName(), restaurant.website(), locations);
	}

	private URI website(String value) {
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

	private record RestaurantSummary(UUID id, String displayName, URI website) {
	}
}
