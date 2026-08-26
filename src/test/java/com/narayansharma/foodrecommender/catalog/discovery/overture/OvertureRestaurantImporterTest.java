package com.narayansharma.foodrecommender.catalog.discovery.overture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchSourceException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "catalog.overture.minimum-records=1")
class OvertureRestaurantImporterTest {
	@Autowired
	private OvertureRestaurantImporter importer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM restaurant_source_records WHERE source = 'overture'");
	}

	@Test
	void importsOnlyOpenFoodPlacesInsideGreaterBoston() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/overture/greater-boston-sample.geojson")) {
			int imported = importer.importSnapshot(input);

			assertThat(imported).isEqualTo(2);
		}

		assertThat(jdbcTemplate.queryForList("""
				SELECT external_id FROM restaurant_source_records
				WHERE source = 'overture'
				ORDER BY external_id
				""", String.class))
				.containsExactly("boston-cafe", "cambridge-restaurant");
		assertThat(jdbcTemplate.queryForObject("""
				SELECT normalized_name FROM restaurant_source_records
				WHERE source = 'overture' AND external_id = 'cambridge-restaurant'
				""", String.class))
				.isEqualTo("cafe sample");
	}

	@Test
	void keepsPreviousSnapshotWhenReplacementIsInvalid() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/overture/greater-boston-sample.geojson")) {
			importer.importSnapshot(input);
		}

		assertThatThrownBy(() -> importer.importSnapshot(new ByteArrayInputStream(
				"{\"type\":\"FeatureCollection\"}".getBytes(StandardCharsets.UTF_8))))
				.isInstanceOf(RestaurantSearchSourceException.class);

		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM restaurant_source_records WHERE source = 'overture'
				""", Integer.class))
				.isEqualTo(2);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "OVERTURE_SMOKE_TEST", matches = "true")
	void importsDownloadedGreaterBostonSnapshot() throws Exception {
		Path snapshot = Path.of(System.getenv("OVERTURE_PLACES_FILE"));

		try (InputStream input = Files.newInputStream(snapshot)) {
			assertThat(importer.importSnapshot(input)).isGreaterThan(100);
		}
	}
}
