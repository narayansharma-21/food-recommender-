package com.narayansharma.foodrecommender.identity.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
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

	@TestConfiguration(proxyBeanMethods = false)
	static class TestAuthConfiguration {
		@Bean
		IdentityTokenVerifier testIdentityTokenVerifier() {
			return token -> {
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
