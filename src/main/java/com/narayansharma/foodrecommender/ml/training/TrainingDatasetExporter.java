package com.narayansharma.foodrecommender.ml.training;

import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TrainingDatasetExporter {
	private static final Logger log = LoggerFactory.getLogger(TrainingDatasetExporter.class);
	private static final String STORAGE_NAMESPACE = "ml-datasets";

	private final TrainingDatasetQuery datasetQuery;
	private final ObjectStorage objectStorage;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public TrainingDatasetExporter(
			TrainingDatasetQuery datasetQuery,
			ObjectStorage objectStorage,
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper,
			Clock clock) {
		this.datasetQuery = datasetQuery;
		this.objectStorage = objectStorage;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional
	public ExportedTrainingDataset export(
			Instant fromInclusive,
			Instant cutoffExclusive,
			String codeVersion) {
		validate(fromInclusive, cutoffExclusive, codeVersion);
		fromInclusive = fromInclusive.truncatedTo(ChronoUnit.MICROS);
		cutoffExclusive = cutoffExclusive.truncatedTo(ChronoUnit.MICROS);
		validate(fromInclusive, cutoffExclusive, codeVersion);
		List<ExportedTrainingDataset> existing = findExisting(fromInclusive, cutoffExclusive, codeVersion);
		if (!existing.isEmpty()) {
			return existing.getFirst();
		}
		List<TrainingExample> examples = datasetQuery.examples(fromInclusive, cutoffExclusive);
		byte[] jsonLines = serialize(examples);
		requireTransactionSynchronization();
		StoredObject stored = store(jsonLines);
		deleteObjectIfTransactionRollsBack(stored.key());
		ExportedTrainingDataset dataset = new ExportedTrainingDataset(
				UUID.randomUUID(),
				TrainingDatasetQuery.SCHEMA_VERSION,
				fromInclusive,
				cutoffExclusive,
				stored.key(),
				stored.sha256(),
				examples.size(),
				codeVersion,
				clock.instant().truncatedTo(ChronoUnit.MICROS));
		insert(dataset);
		return dataset;
	}

	private byte[] serialize(List<TrainingExample> examples) {
		StringBuilder content = new StringBuilder();
		try {
			for (TrainingExample example : examples) {
				content.append(objectMapper.writeValueAsString(example)).append('\n');
			}
		} catch (JacksonException exception) {
			throw new IllegalStateException("Training examples could not be serialized", exception);
		}
		return content.toString().getBytes(StandardCharsets.UTF_8);
	}

	private StoredObject store(byte[] content) {
		try {
			return objectStorage.store(STORAGE_NAMESPACE, new ByteArrayInputStream(content));
		} catch (IOException exception) {
			throw new IllegalStateException("Training dataset could not be stored", exception);
		}
	}

	private void insert(ExportedTrainingDataset dataset) {
		jdbcTemplate.update("""
				INSERT INTO ml_training_datasets (
				    id, schema_version, from_inclusive, cutoff_exclusive, object_key,
				    sha256, row_count, code_version, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
				dataset.id(),
				dataset.schemaVersion(),
				Timestamp.from(dataset.fromInclusive()),
				Timestamp.from(dataset.cutoffExclusive()),
				dataset.objectKey(),
				dataset.sha256(),
				dataset.rowCount(),
				dataset.codeVersion(),
				Timestamp.from(dataset.createdAt()));
	}

	private List<ExportedTrainingDataset> findExisting(
			Instant fromInclusive, Instant cutoffExclusive, String codeVersion) {
		return jdbcTemplate.query("""
				SELECT * FROM ml_training_datasets
				WHERE schema_version = ? AND from_inclusive = ?
				  AND cutoff_exclusive = ? AND code_version = ?
				""", (resultSet, rowNumber) -> new ExportedTrainingDataset(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("schema_version"),
				resultSet.getTimestamp("from_inclusive").toInstant(),
				resultSet.getTimestamp("cutoff_exclusive").toInstant(),
				resultSet.getString("object_key"),
				resultSet.getString("sha256"),
				resultSet.getInt("row_count"),
				resultSet.getString("code_version"),
				resultSet.getTimestamp("created_at").toInstant()),
				TrainingDatasetQuery.SCHEMA_VERSION,
				Timestamp.from(fromInclusive),
				Timestamp.from(cutoffExclusive),
				codeVersion);
	}

	private void deleteObjectIfTransactionRollsBack(String objectKey) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					try {
						objectStorage.delete(objectKey);
					} catch (IOException | RuntimeException exception) {
						log.warn("Could not remove rolled-back ML dataset object key={}", objectKey, exception);
					}
				}
			}
		});
	}

	private void requireTransactionSynchronization() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("Training dataset export requires an active transaction");
		}
	}

	private void validate(Instant fromInclusive, Instant cutoffExclusive, String codeVersion) {
		if (fromInclusive == null || cutoffExclusive == null || !fromInclusive.isBefore(cutoffExclusive)) {
			throw new IllegalArgumentException("Training dataset time range is invalid");
		}
		if (codeVersion == null || codeVersion.isBlank() || codeVersion.length() > 100) {
			throw new IllegalArgumentException("Training dataset code version is invalid");
		}
	}
}
