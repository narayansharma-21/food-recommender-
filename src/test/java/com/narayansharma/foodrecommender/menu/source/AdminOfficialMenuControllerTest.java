package com.narayansharma.foodrecommender.menu.source;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.narayansharma.foodrecommender.identity.auth.IdentityTokenVerifier;
import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "security.admin.identities=firebase:menu-admin")
@AutoConfigureMockMvc
@Import(AdminOfficialMenuControllerTest.AuthConfiguration.class)
@Transactional
class AdminOfficialMenuControllerTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("83000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("83000000-0000-0000-0000-000000000002");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertLocation() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Admin Kitchen', 'admin kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '30 Main Street', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
	}

	@Test
	void adminRegistersAndQueuesAnOfficialMenu() throws Exception {
		mockMvc.perform(post("/v1/admin/menus/official-sources")
				.header("Authorization", "Bearer admin-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "locationId": "%s",
						  "menuKey": "dinner",
						  "displayName": "Dinner Menu",
						  "sourceType": "OFFICIAL_PDF",
						  "url": "https://menu.example.com/dinner.pdf"
						}
						""".formatted(LOCATION_ID)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.menuId").isNotEmpty())
				.andExpect(jsonPath("$.sourceId").isNotEmpty())
				.andExpect(jsonPath("$.status").value("QUEUED"));

		assertThatCount("SELECT COUNT(*) FROM menu_sources WHERE source_type = 'OFFICIAL_PDF'", 1);
		assertThatCount("SELECT COUNT(*) FROM background_jobs WHERE job_type = 'MENU_SOURCE_FETCH'", 1);
	}

	@Test
	void normalUsersCannotRegisterOfficialMenus() throws Exception {
		mockMvc.perform(post("/v1/admin/menus/official-sources")
				.header("Authorization", "Bearer user-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "locationId": "%s",
						  "menuKey": "main",
						  "displayName": "Main Menu",
						  "sourceType": "OFFICIAL_HTML",
						  "url": "https://menu.example.com"
						}
						""".formatted(LOCATION_ID)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"));
	}

	private void assertThatCount(String sql, int expected) {
		org.assertj.core.api.Assertions.assertThat(
				jdbcTemplate.queryForObject(sql, Integer.class)).isEqualTo(expected);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AuthConfiguration {
		@Bean
		IdentityTokenVerifier menuAdminTokenVerifier() {
			return token -> switch (token) {
				case "admin-token" -> new VerifiedIdentity("firebase", "menu-admin");
				case "user-token" -> new VerifiedIdentity("firebase", "normal-user");
				default -> throw new IllegalArgumentException("Invalid test token");
			};
		}
	}
}
