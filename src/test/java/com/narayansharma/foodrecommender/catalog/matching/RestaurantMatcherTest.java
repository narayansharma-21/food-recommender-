package com.narayansharma.foodrecommender.catalog.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCandidate;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestaurantMatcherTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final ExternalRestaurantId EXTERNAL_ID = new ExternalRestaurantId("overture", "place-1");
	private static final RestaurantAddress ADDRESS = new RestaurantAddress(
			"10 Main Street", null, "Boston", "MA", "02110", "US");
	private static final RestaurantCoordinates COORDINATES = coordinates("42.3551", "-71.0550");

	private final RestaurantMatcher matcher = new RestaurantMatcher();

	@Test
	void trustsAnExistingExternalId() {
		RestaurantMatchResult result = matcher.match(candidate(EXTERNAL_ID, "Renamed Restaurant", null, null, null),
				List.of(known("Original Restaurant", ADDRESS, COORDINATES, null, List.of(EXTERNAL_ID))));

		assertThat(result.level()).isEqualTo(RestaurantMatchLevel.EXACT);
		assertThat(result.location().locationId()).isEqualTo(LOCATION_ID);
		assertThat(result.reasons()).containsExactly("same external ID");
	}

	@Test
	void identifiesALikelyMatchFromIndependentSignals() {
		RestaurantCandidate candidate = candidate(
				new ExternalRestaurantId("overture", "new-place"),
				"Cafe Luna Boston",
				ADDRESS,
				coordinates("42.3552", "-71.0550"),
				"+1 (617) 555-0100");

		RestaurantMatchResult result = matcher.match(candidate,
				List.of(known("Café Luna", ADDRESS, COORDINATES, "6175550100", List.of())));

		assertThat(result.level()).isEqualTo(RestaurantMatchLevel.LIKELY);
		assertThat(result.score()).isGreaterThanOrEqualTo(0.75);
		assertThat(result.reasons()).contains("similar name", "same phone", "same address", "within 75 meters");
	}

	@Test
	void sendsAmbiguousMatchesToReview() {
		KnownRestaurantLocation first = known("Cafe Luna", ADDRESS, COORDINATES, null, List.of());
		KnownRestaurantLocation second = new KnownRestaurantLocation(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"Cafe Luna",
				ADDRESS,
				COORDINATES,
				null,
				List.of());

		RestaurantMatchResult result = matcher.match(
				candidate(new ExternalRestaurantId("overture", "new-place"), "Cafe Luna", ADDRESS, COORDINATES, null),
				List.of(first, second));

		assertThat(result.level()).isEqualTo(RestaurantMatchLevel.REVIEW);
	}

	@Test
	void rejectsWeakNameOnlyMatches() {
		RestaurantMatchResult result = matcher.match(
				candidate(new ExternalRestaurantId("overture", "new-place"), "Cafe Luna", null, null, null),
				List.of(known("Cafe Luna", ADDRESS, COORDINATES, null, List.of())));

		assertThat(result.level()).isEqualTo(RestaurantMatchLevel.NONE);
	}

	@Test
	void handlesRepeatedWordsInRestaurantNames() {
		RestaurantMatchResult result = matcher.match(
				candidate(new ExternalRestaurantId("overture", "new-place"), "Pizza Pizza", ADDRESS, null, null),
				List.of(known("Pizza Pizza", ADDRESS, null, null, List.of())));

		assertThat(result.level()).isEqualTo(RestaurantMatchLevel.REVIEW);
	}

	private RestaurantCandidate candidate(
			ExternalRestaurantId externalId,
			String name,
			RestaurantAddress address,
			RestaurantCoordinates coordinates,
			String phone) {
		return new RestaurantCandidate(externalId, name, address, coordinates, phone, URI.create("https://example.com"));
	}

	private KnownRestaurantLocation known(
			String name,
			RestaurantAddress address,
			RestaurantCoordinates coordinates,
			String phone,
			List<ExternalRestaurantId> externalIds) {
		return new KnownRestaurantLocation(
				RESTAURANT_ID, LOCATION_ID, name, address, coordinates, phone, externalIds);
	}

	private static RestaurantCoordinates coordinates(String latitude, String longitude) {
		return new RestaurantCoordinates(new BigDecimal(latitude), new BigDecimal(longitude));
	}
}
