package com.narayansharma.foodrecommender.identity.auth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminIdentityPolicy {
	private final Set<String> identities;

	public AdminIdentityPolicy(@Value("${security.admin.identities:}") String configuredIdentities) {
		this.identities = Arrays.stream(configuredIdentities.split(","))
				.map(String::strip)
				.filter(value -> !value.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean isAdmin(String provider, String subject) {
		if (provider == null || subject == null) {
			return false;
		}
		return identities.contains(provider + ":" + subject);
	}
}
