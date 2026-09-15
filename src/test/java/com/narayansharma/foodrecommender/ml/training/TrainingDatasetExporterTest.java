package com.narayansharma.foodrecommender.ml.training;

import static org.assertj.core.api.Assertions.assertThat;

import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
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
@Transactional
@Import(TrainingDatasetExporterTest.StorageConfiguration.class)
class TrainingDatasetExporterTest {
	@Autowired
	private TrainingDatasetExporter exporter;

	@Autowired
	private InMemoryObjectStorage objectStorage;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesHashedJsonLinesMetadataAndReusesTheSameBuild() throws Exception {
		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant cutoff = Instant.parse("2026-02-01T00:00:00Z");

		ExportedTrainingDataset first = exporter.export(from, cutoff, "git-test");
		ExportedTrainingDataset repeated = exporter.export(from, cutoff, "git-test");

		assertThat(first.rowCount()).isZero();
		assertThat(first.sha256())
				.isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
		assertThat(repeated).isEqualTo(first);
		assertThat(objectStorage.objects).hasSize(1);
		try (InputStream content = objectStorage.load(first.objectKey())) {
			assertThat(content.readAllBytes()).isEmpty();
		}
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM ml_training_datasets", Integer.class)).isEqualTo(1);
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
