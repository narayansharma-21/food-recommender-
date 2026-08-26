package com.narayansharma.foodrecommender.catalog.discovery.overture;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCandidate;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchPage;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchQuery;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchSource;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantTextNormalizer;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OvertureRestaurantSearchSource implements RestaurantSearchSource {
	private static final Base64.Decoder CURSOR_DECODER = Base64.getUrlDecoder();
	private static final Base64.Encoder CURSOR_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final int MAXIMUM_OFFSET = 1_000_000;
	private static final String SELECT = """
			SELECT external_id, display_name, address_line_1, address_line_2, city,
			       region, postal_code, country_code, latitude, longitude, phone,
			       website_url
			FROM restaurant_source_records
			WHERE source = ?
			""";

	private final JdbcTemplate jdbcTemplate;
	private final String launchAreaName;
	private final String launchRegion;
	private final String launchCountryCode;

	public OvertureRestaurantSearchSource(
			JdbcTemplate jdbcTemplate,
			@Value("${catalog.launch-area.name:Greater Boston}") String launchAreaName,
			@Value("${catalog.launch-area.region:MA}") String launchRegion,
			@Value("${catalog.launch-area.country-code:US}") String launchCountryCode) {
		this.jdbcTemplate = jdbcTemplate;
		this.launchAreaName = launchAreaName;
		this.launchRegion = launchRegion;
		this.launchCountryCode = launchCountryCode;
	}

	@Override
	public RestaurantSearchPage search(RestaurantSearchQuery query) {
		if (!launchRegion.equalsIgnoreCase(query.region())
				|| !launchCountryCode.equalsIgnoreCase(query.countryCode())) {
			return new RestaurantSearchPage(List.of(), null);
		}

		String normalizedText = RestaurantTextNormalizer.normalize(query.text());
		if (normalizedText.isEmpty()) {
			return new RestaurantSearchPage(List.of(), null);
		}

		int offset = decodeCursor(query.cursor());
		List<Object> parameters = new ArrayList<>();
		parameters.add(OvertureRestaurantImporter.SOURCE);
		StringBuilder sql = new StringBuilder(SELECT);

		for (String token : normalizedText.split(" ")) {
			sql.append(" AND normalized_name LIKE ?");
			parameters.add("%" + token + "%");
		}
		if (!launchAreaName.equalsIgnoreCase(query.city())) {
			sql.append(" AND LOWER(city) = LOWER(?)");
			parameters.add(query.city());
		}
		sql.append(" ORDER BY confidence DESC NULLS LAST, display_name, external_id LIMIT ? OFFSET ?");
		parameters.add(query.limit() + 1);
		parameters.add(offset);

		List<RestaurantCandidate> results = jdbcTemplate.query(
				sql.toString(),
				this::mapCandidate,
				parameters.toArray());
		boolean hasMore = results.size() > query.limit();
		List<RestaurantCandidate> page = hasMore ? results.subList(0, query.limit()) : results;
		String nextCursor = hasMore ? encodeCursor(offset + query.limit()) : null;
		return new RestaurantSearchPage(page, nextCursor);
	}

	private RestaurantCandidate mapCandidate(ResultSet resultSet, int rowNumber) throws SQLException {
		BigDecimal latitude = resultSet.getBigDecimal("latitude");
		BigDecimal longitude = resultSet.getBigDecimal("longitude");
		RestaurantCoordinates coordinates = latitude == null || longitude == null
				? null
				: new RestaurantCoordinates(latitude, longitude);
		return new RestaurantCandidate(
				new ExternalRestaurantId(
						OvertureRestaurantImporter.SOURCE,
						resultSet.getString("external_id")),
				resultSet.getString("display_name"),
				new RestaurantAddress(
						resultSet.getString("address_line_1"),
						resultSet.getString("address_line_2"),
						resultSet.getString("city"),
						resultSet.getString("region"),
						resultSet.getString("postal_code"),
						resultSet.getString("country_code")),
				coordinates,
				resultSet.getString("phone"),
				parseWebsite(resultSet.getString("website_url")));
	}

	private URI parseWebsite(String website) {
		if (website == null || website.isBlank()) {
			return null;
		}
		try {
			URI uri = URI.create(website);
			return uri.isAbsolute() && ("http".equalsIgnoreCase(uri.getScheme())
					|| "https".equalsIgnoreCase(uri.getScheme())) ? uri : null;
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private int decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return 0;
		}
		try {
			String decoded = new String(CURSOR_DECODER.decode(cursor), StandardCharsets.UTF_8);
			int offset = Integer.parseInt(decoded);
			if (offset < 0 || offset > MAXIMUM_OFFSET) {
				throw new IllegalArgumentException("Restaurant search cursor is invalid");
			}
			return offset;
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Restaurant search cursor is invalid");
		}
	}

	private String encodeCursor(int offset) {
		return CURSOR_ENCODER.encodeToString(Integer.toString(offset).getBytes(StandardCharsets.UTF_8));
	}
}
