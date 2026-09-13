package com.narayansharma.foodrecommender.catalog.dish.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HashingDishEmbeddingProvider implements DishEmbeddingProvider {
	private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

	private final int dimensions;
	private final String modelVersion;

	public HashingDishEmbeddingProvider(
			@Value("${dish.embeddings.local.dimensions:128}") int dimensions,
			@Value("${dish.embeddings.local.model-version:hashing-v1}") String modelVersion) {
		if (dimensions < 16 || dimensions > 2_048) {
			throw new IllegalArgumentException("Local dish embedding dimensions are invalid");
		}
		if (modelVersion == null || modelVersion.isBlank() || modelVersion.length() > 100) {
			throw new IllegalArgumentException("Local dish embedding model version is invalid");
		}
		this.dimensions = dimensions;
		this.modelVersion = modelVersion;
	}

	@Override
	public DishEmbedding embed(String text) {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("Dish embedding text is required");
		}
		double[] vector = new double[dimensions];
		Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
		int tokens = 0;
		while (matcher.find()) {
			byte[] hash = sha256(matcher.group());
			int bucketSource = ((hash[0] & 0xff) << 24)
					| ((hash[1] & 0xff) << 16)
					| ((hash[2] & 0xff) << 8)
					| (hash[3] & 0xff);
			int bucket = Math.floorMod(bucketSource, dimensions);
			vector[bucket] += 1;
			tokens++;
		}
		if (tokens == 0) {
			throw new IllegalArgumentException("Dish embedding text has no searchable tokens");
		}
		normalize(vector);
		return new DishEmbedding("local_hashing", modelVersion, vector);
	}

	private byte[] sha256(String token) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private void normalize(double[] vector) {
		double magnitude = Math.sqrt(java.util.Arrays.stream(vector)
				.map(value -> value * value)
				.sum());
		for (int index = 0; index < vector.length; index++) {
			vector[index] /= magnitude;
		}
	}
}
