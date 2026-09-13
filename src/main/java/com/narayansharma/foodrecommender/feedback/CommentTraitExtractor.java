package com.narayansharma.foodrecommender.feedback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CommentTraitExtractor {
	static final String VERSION = "comment-rules-v1";
	private static final List<String> TRAITS = List.of(
			"crispy", "creamy", "fresh", "savory", "spicy", "tender", "salty", "sweet");

	public List<ExtractedTraitSignal> extract(String comment) {
		if (comment == null || comment.isBlank()) {
			return List.of();
		}
		String normalized = comment.toLowerCase(Locale.ROOT);
		return TRAITS.stream()
				.map(trait -> extract(normalized, trait))
				.filter(java.util.Objects::nonNull)
				.toList();
	}

	private ExtractedTraitSignal extract(String comment, String trait) {
		Pattern negativePattern = Pattern.compile("\\b(?:not|too|overly)\\s+" + trait + "\\b");
		Matcher negative = negativePattern.matcher(comment);
		if (negative.find()) {
			return signal(trait, "NEGATIVE", "0.8500", negative.group());
		}
		Matcher mention = Pattern.compile("\\b" + trait + "\\b").matcher(comment);
		if (mention.find()) {
			return signal(trait, "POSITIVE", "0.7000", mention.group());
		}
		return null;
	}

	private ExtractedTraitSignal signal(
			String trait,
			String sentiment,
			String confidence,
			String evidence) {
		return new ExtractedTraitSignal(
				trait, sentiment, new BigDecimal(confidence), evidence, VERSION);
	}
}
