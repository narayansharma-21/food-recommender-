package com.narayansharma.foodrecommender.catalog.dish.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class DishEmbeddingService {
	private final JdbcTemplate jdbcTemplate;
	private final DishEmbeddingProvider provider;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public DishEmbeddingService(
			JdbcTemplate jdbcTemplate,
			DishEmbeddingProvider provider,
			ObjectMapper objectMapper,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.provider = provider;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional
	public StoredDishEmbedding regenerate(UUID dishConceptId) {
		if (dishConceptId == null) {
			throw new IllegalArgumentException("Dish concept is required");
		}
		DishConcept concept = lockDishConcept(dishConceptId);
		String input = buildInput(dishConceptId, concept);
		String inputHash = sha256(input);
		DishEmbedding embedding = provider.embed(input);
		double[] vector = embedding.vector();

		List<StoredDishEmbedding> existing = findExisting(dishConceptId, embedding, inputHash);
		if (!existing.isEmpty()) {
			StoredDishEmbedding stored = existing.getFirst();
			return new StoredDishEmbedding(
					stored.id(),
					stored.dishConceptId(),
					stored.provider(),
					stored.modelVersion(),
					stored.dimensions(),
					stored.inputSha256(),
					stored.createdAt(),
					false);
		}

		UUID embeddingId = UUID.randomUUID();
		Instant createdAt = clock.instant();
		jdbcTemplate.update("""
				INSERT INTO dish_embeddings (
				    id, dish_concept_id, provider, model_version, dimensions,
				    vector_json, input_sha256, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				embeddingId,
				dishConceptId,
				embedding.provider(),
				embedding.modelVersion(),
				vector.length,
				serialize(vector),
				inputHash,
				Timestamp.from(createdAt));
		return new StoredDishEmbedding(
				embeddingId,
				dishConceptId,
				embedding.provider(),
				embedding.modelVersion(),
				vector.length,
				inputHash,
				createdAt,
				true);
	}

	private DishConcept lockDishConcept(UUID dishConceptId) {
		List<DishConcept> concepts = jdbcTemplate.query("""
				SELECT display_name, normalized_name, description
				FROM dish_concepts
				WHERE id = ?
				FOR UPDATE
				""",
				(resultSet, rowNumber) -> new DishConcept(
						resultSet.getString("display_name"),
						resultSet.getString("normalized_name"),
						resultSet.getString("description")),
				dishConceptId);
		return concepts.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown dish concept: " + dishConceptId));
	}

	private String buildInput(UUID dishConceptId, DishConcept concept) {
		StringBuilder input = new StringBuilder();
		append(input, "display_name", concept.displayName());
		append(input, "normalized_name", concept.normalizedName());
		append(input, "description", concept.description());
		appendAll(input, "ingredients", attributeNames("""
				SELECT ingredient.display_name
				FROM dish_concept_ingredients link
				JOIN ingredients ingredient ON ingredient.id = link.ingredient_id
				WHERE link.dish_concept_id = ?
				ORDER BY ingredient.ingredient_key
				""", dishConceptId));
		appendAll(input, "cuisines", attributeNames("""
				SELECT cuisine.display_name
				FROM dish_concept_cuisines link
				JOIN cuisines cuisine ON cuisine.id = link.cuisine_id
				WHERE link.dish_concept_id = ?
				ORDER BY cuisine.cuisine_key
				""", dishConceptId));
		appendAll(input, "preparations", attributeNames("""
				SELECT preparation.display_name
				FROM dish_concept_preparations link
				JOIN preparations preparation ON preparation.id = link.preparation_id
				WHERE link.dish_concept_id = ?
				ORDER BY preparation.preparation_key
				""", dishConceptId));
		appendAll(input, "traits", attributeNames("""
				SELECT trait.display_name
				FROM dish_concept_traits link
				JOIN dish_traits trait ON trait.id = link.trait_id
				WHERE link.dish_concept_id = ?
				ORDER BY trait.trait_key
				""", dishConceptId));
		return input.toString();
	}

	private List<String> attributeNames(String sql, UUID dishConceptId) {
		return jdbcTemplate.query(
				sql,
				(resultSet, rowNumber) -> resultSet.getString(1),
				dishConceptId);
	}

	private void appendAll(StringBuilder input, String label, List<String> values) {
		for (String value : values) {
			append(input, label, value);
		}
	}

	private void append(StringBuilder input, String label, String value) {
		String safeValue = value == null ? "" : value;
		input.append(label)
				.append(':')
				.append(safeValue.length())
				.append(':')
				.append(safeValue)
				.append('\n');
	}

	private List<StoredDishEmbedding> findExisting(
			UUID dishConceptId,
			DishEmbedding embedding,
			String inputHash) {
		return jdbcTemplate.query("""
				SELECT id, dimensions, created_at
				FROM dish_embeddings
				WHERE dish_concept_id = ? AND provider = ?
				  AND model_version = ? AND input_sha256 = ?
				""",
				(resultSet, rowNumber) -> new StoredDishEmbedding(
						resultSet.getObject("id", UUID.class),
						dishConceptId,
						embedding.provider(),
						embedding.modelVersion(),
						resultSet.getInt("dimensions"),
						inputHash,
						resultSet.getTimestamp("created_at").toInstant(),
						false),
				dishConceptId,
				embedding.provider(),
				embedding.modelVersion(),
				inputHash);
	}

	private String sha256(String input) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private String serialize(double[] vector) {
		try {
			return objectMapper.writeValueAsString(vector);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Dish embedding could not be serialized", exception);
		}
	}

	private record DishConcept(String displayName, String normalizedName, String description) {
	}
}
