package com.narayansharma.foodrecommender;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FoodRecommenderBackendApplicationTests {
	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads() {
		assertThat(flyway.info().applied()).isNotEmpty();
	}

}
