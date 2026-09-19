package com.narayansharma.foodrecommender.administration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AdminAuditSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesAttributableAdminActions() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns "
						+ "WHERE table_name = 'ADMIN_AUDIT_EVENTS'",
				String.class);

		assertThat(columns).contains(
				"ACTOR_USER_ID", "ACTION_TYPE", "TARGET_TYPE", "TARGET_ID", "REASON", "DETAILS_JSON");
	}
}
