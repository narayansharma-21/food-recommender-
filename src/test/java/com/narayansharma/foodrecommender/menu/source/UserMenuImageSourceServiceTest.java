package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

@SpringBootTest(properties = "menu.upload.max-bytes=16")
@Import(UserMenuImageSourceServiceTest.StorageConfiguration.class)
@Transactional
class UserMenuImageSourceServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("80000000-0000-0000-0000-000000000002");
	private static final UUID MENU_ID = UUID.fromString("80000000-0000-0000-0000-000000000003");
	private static final byte[] PNG = {
			(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01
	};

	@Autowired
	private UserMenuImageSourceService service;

	@Autowired
	private InMemoryObjectStorage objectStorage;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertMenu() {
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
	}

	@Test
	void validatesStoresAndRegistersAPngUpload() {
		UploadedMenuImageSource uploaded = service.upload(
				MENU_ID, "user-123", "image/png", new ByteArrayInputStream(PNG));
		UploadedMenuImageSource secondUpload = service.upload(
				MENU_ID, "user-123", "image/png", new ByteArrayInputStream(PNG));

		assertThat(uploaded.objectKey()).startsWith("menu-images/");
		assertThat(uploaded.sizeBytes()).isEqualTo(PNG.length);
		assertThat(objectStorage.objects).containsKey(uploaded.objectKey());
		assertThat(value("SELECT source_type FROM menu_sources WHERE id = ?", uploaded.sourceId()))
				.isEqualTo("USER_IMAGE");
		assertThat(value("SELECT media_type FROM menu_sources WHERE id = ?", uploaded.sourceId()))
				.isEqualTo("image/png");
		assertThat(value("SELECT content_sha256 FROM menu_versions WHERE id = ?", uploaded.versionId()))
				.isEqualTo(uploaded.sha256());
		assertThat(number("SELECT version_number FROM menu_versions WHERE id = ?", uploaded.versionId()))
				.isEqualTo(1);
		assertThat(number("SELECT version_number FROM menu_versions WHERE id = ?", secondUpload.versionId()))
				.isEqualTo(2);
	}

	@Test
	void rejectsContentThatDoesNotMatchTheClaimedType() {
		assertThatThrownBy(() -> service.upload(
				MENU_ID, "user-123", "image/jpeg", new ByteArrayInputStream(PNG)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not match");
		assertThat(objectStorage.objects).isEmpty();
	}

	@Test
	void rejectsImagesAboveTheConfiguredLimit() {
		byte[] oversized = new byte[17];
		System.arraycopy(PNG, 0, oversized, 0, PNG.length);

		assertThatThrownBy(() -> service.upload(
				MENU_ID, "user-123", "image/png", new ByteArrayInputStream(oversized)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("size limit");
		assertThat(objectStorage.objects).isEmpty();
	}

	private String value(String sql, UUID id) {
		return jdbcTemplate.queryForObject(sql, String.class, id);
	}

	private Integer number(String sql, UUID id) {
		return jdbcTemplate.queryForObject(sql, Integer.class, id);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class StorageConfiguration {
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
