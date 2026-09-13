package com.narayansharma.foodrecommender.catalog.dish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DishEmbeddingSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void versionsEmbeddingsByDishProviderModelAndInput() {
		UUID dishId = insertDish();
		insertEmbedding(UUID.randomUUID(), dishId, "a".repeat(64));

		assertThatThrownBy(() -> insertEmbedding(UUID.randomUUID(), dishId, "a".repeat(64)))
				.isInstanceOf(DuplicateKeyException.class);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM dish_embeddings WHERE dish_concept_id = ?",
				Integer.class,
				dishId)).isEqualTo(1);
	}

	private UUID insertDish() {
		UUID dishId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, description, created_at, updated_at
				) VALUES (?, ?, 'Schema Test Dish', 'schema test dish', NULL,
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishId, "schema_test_" + dishId);
		return dishId;
	}

	private void insertEmbedding(UUID embeddingId, UUID dishId, String inputHash) {
		jdbcTemplate.update("""
				INSERT INTO dish_embeddings (
				    id, dish_concept_id, provider, model_version, dimensions,
				    vector_json, input_sha256, created_at
				) VALUES (?, ?, 'local_hashing', 'schema-v1', 16, '[]', ?, CURRENT_TIMESTAMP)
				""", embeddingId, dishId, inputHash);
	}
}
