package com.narayansharma.foodrecommender.identity.preferences;

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
class UserRestrictionServiceTest {
	@Autowired
	private UserProvisioningService provisioningService;

	@Autowired
	private UserRestrictionService restrictionService;

	@Test
	void addsUpdatesAndDeactivatesAUserDeclaredRestriction() {
		InternalUser user = provisioningService.findOrCreate(
				new VerifiedIdentity("firebase", "restriction-test-user"));

		UserRestriction created = restrictionService.set(
				user.id(), RestrictionType.ALLERGY, "Tree-Nut", "Tree Nut", true);
		UserRestriction updated = restrictionService.set(
				user.id(), RestrictionType.ALLERGY, "tree_nut", "Tree nuts", true);

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.key()).isEqualTo("tree_nut");
		assertThat(restrictionService.active(user.id()))
				.extracting(UserRestriction::displayName)
				.containsExactly("Tree nuts");

		restrictionService.set(
				user.id(), RestrictionType.ALLERGY, "tree_nut", "Tree nuts", false);
		assertThat(restrictionService.active(user.id())).isEmpty();
	}
}
