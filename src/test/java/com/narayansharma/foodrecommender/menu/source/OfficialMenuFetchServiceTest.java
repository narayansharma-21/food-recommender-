package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.narayansharma.foodrecommender.menu.version.CapturedMenuVersion;
import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(OfficialMenuFetchServiceTest.FetchConfiguration.class)
@Transactional
class OfficialMenuFetchServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("81000000-0000-0000-0000-000000000002");
	private static final UUID MENU_ID = UUID.fromString("81000000-0000-0000-0000-000000000003");
	private static final UUID SOURCE_ID = UUID.fromString("81000000-0000-0000-0000-000000000004");
	private static final URI MENU_URL = URI.create("https://menu.example.com/dinner");
	private static final byte[] MENU_HTML = "<html>Dinner</html>".getBytes(StandardCharsets.UTF_8);

	@Autowired
	private OfficialMenuFetchService service;

	@Autowired
	private OfficialMenuDocumentFetcher documentFetcher;

	@Autowired
	private InMemoryObjectStorage objectStorage;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void insertSource() {
		objectStorage.objects.clear();
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Sample Kitchen', 'sample kitchen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '10 Main Street', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, 'main', 'Main Menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", MENU_ID, LOCATION_ID);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, origin_url, created_at, updated_at
				) VALUES (?, ?, 'OFFICIAL_HTML', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", SOURCE_ID, MENU_ID, MENU_URL.toString());
		when(documentFetcher.fetch(MenuSourceType.OFFICIAL_HTML, MENU_URL))
				.thenReturn(new FetchedOfficialMenu(MENU_URL, "text/html", MENU_HTML));
	}

	@Test
	void storesVersionsAndQueuesExtractionOnlyWhenContentChanges() {
		CapturedMenuVersion first = service.fetch(SOURCE_ID);
		CapturedMenuVersion repeated = service.fetch(SOURCE_ID);
		entityManager.flush();

		assertThat(first.createdNewVersion()).isTrue();
		assertThat(repeated.versionId()).isEqualTo(first.versionId());
		assertThat(repeated.createdNewVersion()).isFalse();
		assertThat(objectStorage.objects).hasSize(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT raw_object_key FROM menu_sources WHERE id = ?",
				String.class,
				SOURCE_ID)).startsWith("official-menus/");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT media_type FROM menu_versions WHERE id = ?",
				String.class,
				first.versionId())).isEqualTo("text/html");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM background_jobs WHERE job_type = 'MENU_EXTRACTION'",
				Integer.class)).isOne();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FetchConfiguration {
		@Bean
		@Primary
		OfficialMenuDocumentFetcher testOfficialMenuDocumentFetcher() {
			return mock(OfficialMenuDocumentFetcher.class);
		}

		@Bean
		@Primary
		InMemoryObjectStorage inMemoryObjectStorage() {
			return new InMemoryObjectStorage();
		}
	}

	static class InMemoryObjectStorage implements ObjectStorage {
		private final Map<String, byte[]> objects = new HashMap<>();

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
