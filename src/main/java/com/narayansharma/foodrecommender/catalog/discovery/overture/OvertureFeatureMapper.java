package com.narayansharma.foodrecommender.catalog.discovery.overture;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantTextNormalizer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
class OvertureFeatureMapper {
	private static final Set<String> FOOD_CATEGORIES = Set.of(
			"bakery",
			"bar",
			"cafe",
			"coffee_shop",
			"deli",
			"food_court",
			"food_truck",
			"ice_cream_shop",
			"pub");

	private final String launchRegion;
	private final String launchCountryCode;
	private final Set<String> municipalities;

	OvertureFeatureMapper(
			@Value("${catalog.launch-area.region:MA}") String launchRegion,
			@Value("${catalog.launch-area.country-code:US}") String launchCountryCode,
			@Value("${catalog.launch-area.municipalities:Boston,Cambridge,Somerville,Brookline,Newton,Quincy,Medford,Everett,Chelsea,Revere,Malden,Watertown,Arlington}")
			String municipalities) {
		this.launchRegion = launchRegion;
		this.launchCountryCode = launchCountryCode;
		this.municipalities = parseMunicipalities(municipalities);
	}

	RestaurantSourceRecord map(JsonNode feature, Instant importedAt) {
		JsonNode properties = feature.path("properties");
		String id = firstNonNull(text(properties, "id"), text(feature, "id"));
		String name = text(properties.path("names"), "primary");
		String category = text(properties.path("categories"), "primary");
		String operatingStatus = text(properties, "operating_status");
		JsonNode address = properties.path("addresses").path(0);

		if (id == null || name == null || !isFoodCategory(category) || !isOpen(operatingStatus)) {
			return null;
		}

		String city = text(address, "locality");
		String region = text(address, "region");
		String country = text(address, "country");
		if (!isInLaunchArea(city, region, country)) {
			return null;
		}

		JsonNode coordinates = feature.path("geometry").path("coordinates");
		BigDecimal longitude = decimal(coordinates, 0);
		BigDecimal latitude = decimal(coordinates, 1);
		if ((latitude == null) != (longitude == null)) {
			latitude = null;
			longitude = null;
		}

		return new RestaurantSourceRecord(
				limit(id, 255),
				limit(name, 200),
				limit(RestaurantTextNormalizer.normalize(name), 200),
				limit(text(address, "freeform"), 200),
				limit(text(address, "unit"), 200),
				limit(city, 100),
				limit(region, 100),
				limit(text(address, "postcode"), 20),
				limit(country, 2),
				latitude,
				longitude,
				limit(firstText(properties.path("phones")), 50),
				limit(firstText(properties.path("websites")), 500),
				limit(category, 100),
				decimal(properties, "confidence"),
				importedAt);
	}

	private boolean isFoodCategory(String category) {
		return category != null
				&& (category.endsWith("_restaurant")
				|| "restaurant".equals(category)
				|| FOOD_CATEGORIES.contains(category));
	}

	private boolean isOpen(String operatingStatus) {
		return operatingStatus == null || "open".equalsIgnoreCase(operatingStatus);
	}

	private boolean isInLaunchArea(String city, String region, String country) {
		return city != null
				&& matchesRegion(region)
				&& (country == null || launchCountryCode.equalsIgnoreCase(country))
				&& municipalities.contains(city.toLowerCase(Locale.ROOT));
	}

	private boolean matchesRegion(String region) {
		return region == null
				|| launchRegion.equalsIgnoreCase(region)
				|| ("MA".equalsIgnoreCase(launchRegion) && "Massachusetts".equalsIgnoreCase(region));
	}

	private Set<String> parseMunicipalities(String configured) {
		Set<String> parsed = new HashSet<>();
		Arrays.stream(configured.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.map(value -> value.toLowerCase(Locale.ROOT))
				.forEach(parsed::add);
		return Set.copyOf(parsed);
	}

	private String firstText(JsonNode array) {
		return array.isArray() && !array.isEmpty() && array.get(0).isString()
				? array.get(0).stringValue()
				: null;
	}

	private String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isString() && !value.stringValue().isBlank() ? value.stringValue() : null;
	}

	private BigDecimal decimal(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isNumber() ? value.decimalValue() : null;
	}

	private BigDecimal decimal(JsonNode array, int index) {
		return array.isArray() && array.size() > index && array.get(index).isNumber()
				? array.get(index).decimalValue()
				: null;
	}

	private String limit(String value, int maximumLength) {
		return value == null || value.length() <= maximumLength ? value : value.substring(0, maximumLength);
	}

	private String firstNonNull(String first, String second) {
		return first != null ? first : second;
	}
}
