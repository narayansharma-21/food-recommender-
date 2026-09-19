package com.narayansharma.foodrecommender.identity.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = "security.admin.identities=firebase:filter-test-user")
@AutoConfigureMockMvc
@Import(IdentityAuthenticationFilterTest.TestAuthConfiguration.class)
class IdentityAuthenticationFilterTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatesAndProvisionsAValidBearerToken() throws Exception {
		mockMvc.perform(get("/v1/test-auth")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("firebase"))
				.andExpect(jsonPath("$.providerSubject").value("filter-test-user"));
	}

	@Test
	void rejectsAnInvalidBearerToken() throws Exception {
		mockMvc.perform(get("/v1/test-auth")
				.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void doesNotGrantOperationalAccessToNormalUsers() throws Exception {
		mockMvc.perform(get("/actuator/metrics")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void leavesPublicHealthChecksIndependentFromUserTokens() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness")
				.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isOk());
	}

	@Test
	void allowsOnlyConfiguredIdentitiesIntoAdminRoutes() throws Exception {
		mockMvc.perform(get("/v1/admin/jobs/failed")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/v1/admin/jobs/failed")
				.header("Authorization", "Bearer normal-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void readsAndUpdatesOnlyTheAuthenticatedUsersProfile() throws Exception {
		mockMvc.perform(get("/v1/users/me")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.homeCity").value("Greater Boston"));

		mockMvc.perform(patch("/v1/users/me")
				.header("Authorization", "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName":"Alex","homeCity":"Cambridge","preferredLocale":"en-US"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Alex"))
				.andExpect(jsonPath("$.homeCity").value("Cambridge"));
	}

	@Test
	void managesOnlyTheAuthenticatedUsersRestrictions() throws Exception {
		mockMvc.perform(put("/v1/users/me/restrictions/ALLERGY/shellfish")
				.header("Authorization", "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName":"Shellfish","active":true}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.key").value("shellfish"));

		mockMvc.perform(get("/v1/users/me/restrictions")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].displayName").value("Shellfish"));
	}

	@Test
	void rejectsUnknownRestrictionTypes() throws Exception {
		mockMvc.perform(put("/v1/users/me/restrictions/UNKNOWN/shellfish")
				.header("Authorization", "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName":"Shellfish","active":true}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void validatesAuthenticatedRatingRequests() throws Exception {
		mockMvc.perform(post("/v1/ratings")
				.header("Authorization", "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"menuItemId":"00000000-0000-0000-0000-000000000001","score":6}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void validatesAuthenticatedRecommendationRequests() throws Exception {
		mockMvc.perform(post("/v1/restaurants/00000000-0000-0000-0000-000000000001/recommendations")
				.header("Authorization", "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"mode":"SAFE_BET","limit":26}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void reportsAnUnknownRatedMenuItem() throws Exception {
		mockMvc.perform(post("/v1/ratings")
				.header("Authorization", "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"menuItemId":"00000000-0000-0000-0000-000000000001","score":5}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MENU_ITEM_NOT_FOUND"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestAuthConfiguration {
		@Bean
		IdentityTokenVerifier testIdentityTokenVerifier() {
			return token -> {
				if ("normal-token".equals(token)) {
					return new VerifiedIdentity("firebase", "normal-test-user");
				}
				if (!"valid-token".equals(token)) {
					throw new InvalidIdentityTokenException("Invalid test token");
				}
				return new VerifiedIdentity("firebase", "filter-test-user");
			};
		}

		@Bean
		TestAuthController testAuthController() {
			return new TestAuthController();
		}
	}

	@RestController
	static class TestAuthController {
		@GetMapping("/v1/test-auth")
		UserPrincipal get(@AuthenticationPrincipal UserPrincipal principal) {
			return principal;
		}
	}
}
