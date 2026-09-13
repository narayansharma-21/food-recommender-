package com.narayansharma.foodrecommender.catalog.dish.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DishEmbeddingServiceTest {
	@Autowired
	private DishEmbeddingService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void reusesUnchangedInputAndVersionsChangedKnowledgeWithoutReplacingTheDish() {
		UUID dishId = insertDishWithIngredient();

		StoredDishEmbedding first = service.regenerate(dishId);
		StoredDishEmbedding repeated = service.regenerate(dishId);

		assertThat(first.created()).isTrue();
		assertThat(repeated.created()).isFalse();
		assertThat(repeated.id()).isEqualTo(first.id());
		assertThat(first.dishConceptId()).isEqualTo(dishId);
		assertThat(first.provider()).isEqualTo("local_hashing");
		assertThat(first.dimensions()).isEqualTo(128);

		jdbcTemplate.update(
				"UPDATE dish_concepts SET description = 'Now described as creamy' WHERE id = ?",
				dishId);
		StoredDishEmbedding changed = service.regenerate(dishId);

		assertThat(changed.created()).isTrue();
		assertThat(changed.id()).isNotEqualTo(first.id());
		assertThat(changed.dishConceptId()).isEqualTo(dishId);
		assertThat(changed.inputSha256()).isNotEqualTo(first.inputSha256());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM dish_embeddings WHERE dish_concept_id = ?",
				Integer.class,
				dishId)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM dish_concepts WHERE id = ?",
				Integer.class,
				dishId)).isEqualTo(1);
	}

	private UUID insertDishWithIngredient() {
		UUID dishId = UUID.randomUUID();
		UUID ingredientId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, description, created_at, updated_at
				) VALUES (?, ?, 'Clam Chowder', 'clam chowder', 'New England soup',
				          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishId, "embedding_test_" + dishId);
		jdbcTemplate.update("""
				INSERT INTO ingredients (
				    id, ingredient_key, display_name, normalized_name, created_at, updated_at
				) VALUES (?, ?, 'Clam', 'clam', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", ingredientId, "embedding_test_" + ingredientId);
		jdbcTemplate.update(
				"INSERT INTO dish_concept_ingredients (dish_concept_id, ingredient_id) VALUES (?, ?)",
				dishId,
				ingredientId);
		return dishId;
	}
}
