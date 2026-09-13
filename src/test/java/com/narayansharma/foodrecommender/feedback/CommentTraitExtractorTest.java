package com.narayansharma.foodrecommender.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class CommentTraitExtractorTest {
	private final CommentTraitExtractor extractor = new CommentTraitExtractor();

	@Test
	void extractsPositiveAndNegativeTraitMentions() {
		assertThat(extractor.extract("Fresh and crispy, but too salty"))
				.extracting(ExtractedTraitSignal::traitKey, ExtractedTraitSignal::sentiment)
				.containsExactly(
						tuple("crispy", "POSITIVE"),
						tuple("fresh", "POSITIVE"),
						tuple("salty", "NEGATIVE"));
	}

	@Test
	void doesNotInventSignalsWithoutTraitWords() {
		assertThat(extractor.extract("I would order this again")).isEmpty();
		assertThat(extractor.extract(" ")).isEmpty();
	}
}
