package com.narayansharma.foodrecommender.menu.source;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OfficialMenuUrlPolicy {
	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
	private static final Pattern IPV4_LITERAL = Pattern.compile("[0-9]{1,3}(\\.[0-9]{1,3}){3}");

	public URI validateAndNormalize(URI url) {
		if (url == null || !url.isAbsolute() || url.getScheme() == null || url.getHost() == null) {
			throw invalid();
		}
		String scheme = url.getScheme().toLowerCase(Locale.ROOT);
		String host = url.getHost().toLowerCase(Locale.ROOT);
		if (!ALLOWED_SCHEMES.contains(scheme)
				|| url.getRawUserInfo() != null
				|| url.getFragment() != null
				|| !isPublicHostname(host)
				|| (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443)) {
			throw invalid();
		}
		StringBuilder normalizedValue = new StringBuilder(scheme).append("://").append(host);
		if (url.getPort() != -1) {
			normalizedValue.append(':').append(url.getPort());
		}
		normalizedValue.append(url.getRawPath() == null || url.getRawPath().isEmpty() ? "/" : url.getRawPath());
		if (url.getRawQuery() != null) {
			normalizedValue.append('?').append(url.getRawQuery());
		}
		URI normalized = URI.create(normalizedValue.toString()).normalize();
		if (normalized.toASCIIString().length() > 500) {
			throw invalid();
		}
		return normalized;
	}

	private boolean isPublicHostname(String host) {
		return host.contains(".")
				&& !IPV4_LITERAL.matcher(host).matches()
				&& !host.contains(":")
				&& !host.endsWith(".local")
				&& !host.endsWith(".internal")
				&& !host.endsWith(".localhost");
	}

	private IllegalArgumentException invalid() {
		return new IllegalArgumentException("Official menu URL must be a public HTTP or HTTPS URL");
	}
}
