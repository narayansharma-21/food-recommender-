package com.narayansharma.foodrecommender.catalog.dish;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DishKnowledgeSchemaTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void linksReusableKnowledgeToADishConcept() {
		UUID dishId = UUID.randomUUID();
		UUID ingredientId = UUID.randomUUID();
		UUID cuisineId = UUID.randomUUID();
		UUID preparationId = UUID.randomUUID();
		UUID traitId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO dish_concepts (
				    id, concept_key, display_name, normalized_name, description, created_at, updated_at
				) VALUES (?, 'new_england_clam_chowder_schema_test', 'New England Clam Chowder',
				          'new england clam chowder', 'A reusable dish concept', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", dishId);
		jdbcTemplate.update("""
				INSERT INTO ingredients (
				    id, ingredient_key, display_name, normalized_name, created_at, updated_at
				) VALUES (?, 'scallop_schema_test', 'Scallop', 'scallop', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", ingredientId);
		jdbcTemplate.update("""
				INSERT INTO cuisines (id, cuisine_key, display_name, created_at, updated_at)
				VALUES (?, 'new_england_schema_test', 'New England', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", cuisineId);
		jdbcTemplate.update("""
				INSERT INTO preparations (id, preparation_key, display_name, created_at, updated_at)
				VALUES (?, 'soup_schema_test', 'Soup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", preparationId);
		jdbcTemplate.update("""
				INSERT INTO dish_traits (
				    id, trait_key, display_name, trait_category, created_at, updated_at
				) VALUES (?, 'silky_schema_test', 'Silky', 'TEXTURE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", traitId);

		jdbcTemplate.update(
				"INSERT INTO dish_concept_ingredients (dish_concept_id, ingredient_id) VALUES (?, ?)",
				dishId,
				ingredientId);
		jdbcTemplate.update(
				"INSERT INTO dish_concept_cuisines (dish_concept_id, cuisine_id) VALUES (?, ?)",
				dishId,
				cuisineId);
		jdbcTemplate.update(
				"INSERT INTO dish_concept_preparations (dish_concept_id, preparation_id) VALUES (?, ?)",
				dishId,
				preparationId);
		jdbcTemplate.update(
				"INSERT INTO dish_concept_traits (dish_concept_id, trait_id) VALUES (?, ?)",
				dishId,
				traitId);

		assertThat(count("dish_concept_ingredients", dishId)).isEqualTo(1);
		assertThat(count("dish_concept_cuisines", dishId)).isEqualTo(1);
		assertThat(count("dish_concept_preparations", dishId)).isEqualTo(1);
		assertThat(count("dish_concept_traits", dishId)).isEqualTo(1);
	}

	private Integer count(String table, UUID dishId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE dish_concept_id = ?",
				Integer.class,
				dishId);
	}
}
