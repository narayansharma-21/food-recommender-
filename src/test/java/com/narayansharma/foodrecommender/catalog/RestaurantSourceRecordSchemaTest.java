package com.narayansharma.foodrecommender.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantSourceRecordSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesSourceRecordsSeparatelyFromCanonicalRestaurants() {
		jdbcTemplate.update("""
				INSERT INTO restaurant_source_records (
				    source, external_id, display_name, normalized_name, city, region,
				    country_code, latitude, longitude, category, confidence, imported_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
				""",
				"OVERTURE",
				"overture-place-1",
				"Sample Kitchen",
				"sample kitchen",
				"Cambridge",
				"MA",
				"US",
				42.370000,
				-71.110000,
				"restaurant",
				0.900);

		Integer sourceCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM restaurant_source_records WHERE source = 'OVERTURE'",
				Integer.class);
		Integer canonicalCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM restaurants",
				Integer.class);

		assertThat(sourceCount).isEqualTo(1);
		assertThat(canonicalCount).isZero();
	}
}
