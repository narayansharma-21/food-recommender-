package com.narayansharma.foodrecommender.identity.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.identity.InternalUser;
import com.narayansharma.foodrecommender.identity.UserProvisioningService;
import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserProfileServiceTest {
	@Autowired
	private UserProvisioningService provisioningService;

	@Autowired
	private UserProfileService profileService;

	@Test
	void readsAndUpdatesAnActiveUsersProfile() {
		InternalUser user = provisioningService.findOrCreate(
				new VerifiedIdentity("firebase", "profile-test-user"));

		UserProfile initial = profileService.get(user.id());
		UserProfile updated = profileService.update(
				user.id(),
				"  Alex  ",
				" Cambridge ",
				"en-US");

		assertThat(initial.homeCity()).isEqualTo("Greater Boston");
		assertThat(updated.displayName()).isEqualTo("Alex");
		assertThat(updated.homeCity()).isEqualTo("Cambridge");
		assertThat(profileService.get(user.id())).isEqualTo(updated);
	}
}
