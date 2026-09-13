package com.narayansharma.foodrecommender.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class IdentityProvisioningLockSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsTheFixedProvisioningLockBuckets() {
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM identity_provisioning_locks",
				Integer.class)).isEqualTo(16);
	}
}
