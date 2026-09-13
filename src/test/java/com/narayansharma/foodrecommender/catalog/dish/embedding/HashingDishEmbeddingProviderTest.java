package com.narayansharma.foodrecommender.catalog.dish.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashingDishEmbeddingProviderTest {
	private final HashingDishEmbeddingProvider provider =
			new HashingDishEmbeddingProvider(64, "test-v1");

	@Test
	void createsDeterministicNormalizedVectorsLocally() {
		DishEmbedding first = provider.embed("New England clam chowder creamy soup");
		DishEmbedding repeated = provider.embed("New England clam chowder creamy soup");
		DishEmbedding different = provider.embed("Spicy crispy chicken sandwich");

		assertThat(first.provider()).isEqualTo("local_hashing");
		assertThat(first.modelVersion()).isEqualTo("test-v1");
		assertThat(first.vector()).containsExactly(repeated.vector());
		assertThat(java.util.Arrays.equals(first.vector(), different.vector())).isFalse();
		double magnitude = Math.sqrt(java.util.Arrays.stream(first.vector())
				.map(value -> value * value)
				.sum());
		assertThat(magnitude).isCloseTo(1, within(0.000_000_1));
	}

	@Test
	void protectsStoredVectorFromCallerMutation() {
		DishEmbedding embedding = provider.embed("lobster roll");
		double original = embedding.vector()[0];

		embedding.vector()[0] = 99;

		assertThat(embedding.vector()[0]).isEqualTo(original);
	}

	private org.assertj.core.data.Offset<Double> within(double value) {
		return org.assertj.core.data.Offset.offset(value);
	}
}
