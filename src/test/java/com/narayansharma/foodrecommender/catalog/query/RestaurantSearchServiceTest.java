package com.narayansharma.foodrecommender.catalog.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCandidate;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchPage;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchQuery;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchSource;
import com.narayansharma.foodrecommender.catalog.identifiers.RestaurantExternalIdService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestaurantSearchServiceTest {
	private static final ExternalRestaurantId EXTERNAL_ID = new ExternalRestaurantId("overture", "place-1");
	private static final UUID RESTAURANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Test
	void includesCanonicalIdsWhenTheSourceRecordIsLinked() {
		RestaurantCandidate candidate = new RestaurantCandidate(
				EXTERNAL_ID,
				"Cafe Example",
				new RestaurantAddress("1 Main St", null, "Boston", "MA", "02108", "US"),
				null,
				null,
				null);
		RestaurantSearchSource source = query -> new RestaurantSearchPage(List.of(candidate), "next");
		RestaurantExternalIdService ids = org.mockito.Mockito.mock(RestaurantExternalIdService.class);
		org.mockito.Mockito.when(ids.resolve(EXTERNAL_ID)).thenReturn(java.util.Optional.of(
				new com.narayansharma.foodrecommender.catalog.identifiers.ResolvedRestaurantLocation(
						EXTERNAL_ID, RESTAURANT_ID, LOCATION_ID)));

		RestaurantSearchResponse response = new RestaurantSearchService(source, ids).search(
				new RestaurantSearchQuery("cafe", "Greater Boston", "MA", "US", 20, null));

		assertThat(response.nextCursor()).isEqualTo("next");
		assertThat(response.restaurants()).singleElement().satisfies(result -> {
			assertThat(result.restaurantId()).isEqualTo(RESTAURANT_ID);
			assertThat(result.locationId()).isEqualTo(LOCATION_ID);
			assertThat(result.displayName()).isEqualTo("Cafe Example");
		});
	}

	@Test
	void leavesCanonicalIdsEmptyForAnUnlinkedSourceRecord() {
		RestaurantCandidate candidate = new RestaurantCandidate(
				EXTERNAL_ID, "New Place", null, null, null, null);
		RestaurantSearchSource source = query -> new RestaurantSearchPage(List.of(candidate), null);
		RestaurantExternalIdService ids = org.mockito.Mockito.mock(RestaurantExternalIdService.class);
		org.mockito.Mockito.when(ids.resolve(EXTERNAL_ID)).thenReturn(java.util.Optional.empty());

		RestaurantSearchResult result = new RestaurantSearchService(source, ids).search(
				new RestaurantSearchQuery("new", "Greater Boston", "MA", "US", 20, null))
				.restaurants().getFirst();

		assertThat(result.restaurantId()).isNull();
		assertThat(result.locationId()).isNull();
	}
}
