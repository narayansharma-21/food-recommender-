package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class HtmlMenuTextExtractor {
	private static final Pattern COMMENT = Pattern.compile("(?s)<!--.*?-->");
	private static final Pattern NON_CONTENT = Pattern.compile(
			"(?is)<(script|style|noscript)\\b[^>]*>.*?</\\1\\s*>");
	private static final Pattern LINE_BREAK = Pattern.compile(
			"(?i)<\\s*(?:br\\s*/?|/?(?:p|div|li|tr|h[1-6])\\b[^>]*)>");
	private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");

	String extract(byte[] html) {
		if (html == null || html.length == 0) {
			throw new IllegalArgumentException("HTML menu content is required");
		}
		String text = new String(html, StandardCharsets.UTF_8);
		text = COMMENT.matcher(text).replaceAll(" ");
		text = NON_CONTENT.matcher(text).replaceAll(" ");
		text = LINE_BREAK.matcher(text).replaceAll("\n");
		text = TAG.matcher(text).replaceAll(" ");
		text = HtmlUtils.htmlUnescape(text).replace('\u00a0', ' ');
		String normalized = text.lines()
				.map(String::strip)
				.filter(line -> !line.isEmpty())
				.collect(Collectors.joining("\n"));
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("HTML menu contains no readable text");
		}
		return normalized;
	}
}
