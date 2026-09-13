package com.narayansharma.foodrecommender.catalog.dish.embedding;

public interface DishEmbeddingProvider {
	DishEmbedding embed(String text);
}
