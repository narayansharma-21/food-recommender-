package com.narayansharma.foodrecommender.ml.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class TrainingExampleContractTest {
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void javaRecordMatchesTheSharedJsonContract() throws Exception {
		JsonNode schema = objectMapper.readTree(Path.of(
				"ml/contracts/recommendation-training-example-v1.schema.json").toFile());
		JsonNode serialized = objectMapper.valueToTree(example());
		List<String> required = new ArrayList<>();
		schema.get("required").forEach(field -> required.add(field.stringValue()));
		List<String> actual = new ArrayList<>(serialized.propertyNames());

		assertThat(actual).containsExactlyInAnyOrderElementsOf(required);
		assertThat(serialized.get("schemaVersion").stringValue())
				.isEqualTo(schema.at("/properties/schemaVersion/const").stringValue());
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
