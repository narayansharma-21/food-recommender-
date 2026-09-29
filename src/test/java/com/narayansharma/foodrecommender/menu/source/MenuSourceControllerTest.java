package com.narayansharma.foodrecommender.menu.source;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.narayansharma.foodrecommender.identity.auth.IdentityTokenVerifier;
import com.narayansharma.foodrecommender.identity.auth.VerifiedIdentity;
import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"menu.upload.max-bytes=16", "menu.upload.rate-limit.requests=100"})
@AutoConfigureMockMvc
@Import(MenuSourceControllerTest.AuthStorageConfiguration.class)
@Transactional
class MenuSourceControllerTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("35000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("36000000-0000-0000-0000-000000000001");
	private static final byte[] PNG = {
			(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01
	};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertRestaurant() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Upload Cafe', 'upload cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '1 Main St', 'Boston', 'MA', 'US', 'America/New_York',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
	}

	@Test
	void acceptsAnAuthenticatedPngAndQueuesExtraction() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "menu.png", "image/png", PNG);

		mockMvc.perform(multipart("/v1/menus/upload")
				.file(file)
				.param("locationId", LOCATION_ID.toString())
				.header("Authorization", "Bearer menu-token"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.menuId").isNotEmpty())
				.andExpect(jsonPath("$.menuVersionId").isNotEmpty())
				.andExpect(jsonPath("$.extractionStatus").value("PROCESSING"))
				.andExpect(jsonPath("$.mediaType").value("image/png"));
	}

	@Test
	void requiresAuthenticationForUploads() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "menu.png", "image/png", PNG);

		mockMvc.perform(multipart("/v1/menus/upload")
				.file(file)
				.param("locationId", LOCATION_ID.toString()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AuthStorageConfiguration {
		@Bean
		IdentityTokenVerifier menuUploadIdentityTokenVerifier() {
			return token -> {
				if (!"menu-token".equals(token)) {
					throw new IllegalArgumentException("Invalid test token");
				}
				return new VerifiedIdentity("firebase", "menu-upload-user");
			};
		}

		@Bean
		@Primary
		ObjectStorage menuUploadObjectStorage() {
			return new InMemoryObjectStorage();
		}
	}

	private static final class InMemoryObjectStorage implements ObjectStorage {
		private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

		@Override
		public StoredObject store(String namespace, InputStream content) throws IOException {
			byte[] bytes = content.readAllBytes();
			String key = namespace + "/" + UUID.randomUUID();
			objects.put(key, bytes);
			return new StoredObject(key, bytes.length, sha256(bytes));
		}

		@Override
		public InputStream load(String key) {
			return new ByteArrayInputStream(objects.get(key));
		}

		@Override
		public void delete(String key) {
			objects.remove(key);
		}

		private String sha256(byte[] bytes) {
			try {
				return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
			} catch (NoSuchAlgorithmException exception) {
				throw new IllegalStateException(exception);
			}
		}
	}
}
