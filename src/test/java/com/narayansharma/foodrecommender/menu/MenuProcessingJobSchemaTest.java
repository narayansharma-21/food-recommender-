package com.narayansharma.foodrecommender.menu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MenuProcessingJobSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void migrationCreatesTheMenuProcessingJobLink() {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.TABLES
				WHERE TABLE_NAME = 'MENU_PROCESSING_JOBS'
				""", Integer.class);

		assertThat(count).isOne();
	}
}
