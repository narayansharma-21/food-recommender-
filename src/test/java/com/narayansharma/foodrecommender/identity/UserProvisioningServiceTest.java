package com.narayansharma.foodrecommender.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserProvisioningServiceTest {
	@Autowired
	private UserProvisioningService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsOneInternalUserForRepeatedVerifiedIdentity() {
		VerifiedIdentity identity = new VerifiedIdentity("firebase", "provisioning-test-user");

		InternalUser first = service.findOrCreate(identity);
		InternalUser repeated = service.findOrCreate(identity);

		assertThat(first.created()).isTrue();
		assertThat(repeated.created()).isFalse();
		assertThat(repeated.id()).isEqualTo(first.id());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE id = ?",
				Integer.class,
				first.id())).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT home_city FROM user_profiles WHERE user_id = ?",
				String.class,
				first.id())).isEqualTo("Greater Boston");
	}
}
