package com.narayansharma.foodrecommender.catalog.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestaurantSearchContractTest {
	@Test
	void searchPageKeepsAnImmutableResultList() {
		List<RestaurantCandidate> candidates = new ArrayList<>();
		RestaurantSearchPage page = new RestaurantSearchPage(candidates, null);

		candidates.add(null);

		assertThat(page.restaurants()).isEmpty();
		assertThatThrownBy(() -> page.restaurants().add(null))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void queryRejectsInvalidOrUnboundedInputs() {
		assertThatThrownBy(() -> new RestaurantSearchQuery("pizza", "New York", "NY", "us", 20, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("country code");
		assertThatThrownBy(() -> new RestaurantSearchQuery("pizza", "New York", "NY", "US", 51, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("limit");
		assertThatThrownBy(() -> new RestaurantSearchQuery("a b c d e f g h i j k", "Boston", "MA", "US", 20, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("too long");
	}
}
