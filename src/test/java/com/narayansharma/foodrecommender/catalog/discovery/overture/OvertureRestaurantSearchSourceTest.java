package com.narayansharma.foodrecommender.catalog.discovery.overture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchPage;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchQuery;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "catalog.overture.minimum-records=1")
class OvertureRestaurantSearchSourceTest {
	@Autowired
	private OvertureRestaurantImporter importer;

	@Autowired
	private OvertureRestaurantSearchSource source;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void importFixture() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/overture/greater-boston-sample.geojson")) {
			importer.importSnapshot(input);
		}
	}

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM restaurant_source_records WHERE source = 'overture'");
	}

	@Test
	void searchesAcrossGreaterBostonOrWithinOneMunicipality() {
		RestaurantSearchPage broad = source.search(
				new RestaurantSearchQuery("coffee", "Greater Boston", "MA", "US", 20, null));
		RestaurantSearchPage cambridge = source.search(
				new RestaurantSearchQuery("cafe", "Cambridge", "MA", "US", 20, null));
		RestaurantSearchPage wrongCity = source.search(
				new RestaurantSearchQuery("coffee", "Cambridge", "MA", "US", 20, null));

		assertThat(broad.restaurants()).singleElement().satisfies(candidate -> {
			assertThat(candidate.displayName()).isEqualTo("Boston Coffee");
			assertThat(candidate.externalId().source()).isEqualTo("overture");
		});
		assertThat(cambridge.restaurants()).singleElement().satisfies(candidate ->
				assertThat(candidate.displayName()).isEqualTo("Café Sample"));
		assertThat(wrongCity.restaurants()).isEmpty();
	}

	@Test
	void usesAnOpaqueCursorForPagination() {
		RestaurantSearchPage first = source.search(
				new RestaurantSearchQuery("c", "Greater Boston", "MA", "US", 1, null));
		RestaurantSearchPage second = source.search(
				new RestaurantSearchQuery("c", "Greater Boston", "MA", "US", 1, first.nextCursor()));

		assertThat(first.restaurants()).hasSize(1);
		assertThat(first.nextCursor()).isNotBlank();
		assertThat(second.restaurants()).hasSize(1);
		assertThat(second.restaurants().getFirst()).isNotEqualTo(first.restaurants().getFirst());
		assertThat(second.nextCursor()).isNull();
	}

	@Test
	void rejectsAnInvalidCursor() {
		assertThatThrownBy(() -> source.search(
				new RestaurantSearchQuery("coffee", "Greater Boston", "MA", "US", 20, "not-a-cursor")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("cursor");
	}
}
