package com.narayansharma.foodrecommender.ml.training;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrainingExampleContractTest {
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void javaRecordMatchesTheSharedJsonContract() throws Exception {
		JsonNode schema = objectMapper.readTree(Path.of(
				"ml/contracts/recommendation-training-example-v1.schema.json").toFile());
		JsonNode serialized = objectMapper.valueToTree(example());
		List<String> required = new ArrayList<>();
		schema.get("required").forEach(field -> required.add(field.asText()));
		List<String> actual = new ArrayList<>();
		serialized.fieldNames().forEachRemaining(actual::add);

		assertThat(actual).containsExactlyInAnyOrderElementsOf(required);
		assertThat(serialized.get("schemaVersion").asText())
				.isEqualTo(schema.at("/properties/schemaVersion/const").asText());
	}

	private TrainingExample example() {
		return new TrainingExample(
				"recommendation-training-example-v1",
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				null,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z"),
				"SAFE_BET",
				"weighted-rules-v1",
				"no-profile-v1",
				1,
				BigDecimal.ZERO,
				new BigDecimal("0.625000"),
				0,
				0,
				false,
				4,
				true);
	}
}
