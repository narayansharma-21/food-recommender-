package com.narayansharma.foodrecommender.identity.consent;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.identity.InternalUser;
import com.narayansharma.foodrecommender.identity.UserProvisioningService;
import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserConsentServiceTest {
	@Autowired
	private UserProvisioningService provisioningService;

	@Autowired
	private UserConsentService consentService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void preservesConsentHistoryAndReturnsTheLatestChoice() {
		InternalUser user = provisioningService.findOrCreate(
				new VerifiedIdentity("firebase", "consent-test-user"));

		UserConsent granted = consentService.record(
				user.id(), ConsentType.LOCATION_DATA, true, "privacy-v1");
		UserConsent revoked = consentService.record(
				user.id(), ConsentType.LOCATION_DATA, false, "privacy-v1");

		assertThat(granted.sequence()).isEqualTo(1);
		assertThat(revoked.sequence()).isEqualTo(2);
		assertThat(consentService.current(user.id(), ConsentType.LOCATION_DATA))
				.contains(revoked);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM user_consent_events WHERE user_id = ?",
				Integer.class,
				user.id())).isEqualTo(2);
	}
}
