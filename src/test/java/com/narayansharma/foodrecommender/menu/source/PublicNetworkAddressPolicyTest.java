package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicNetworkAddressPolicyTest {
	@Test
	void acceptsOnlyPublicDnsResults() throws Exception {
		PublicNetworkAddressPolicy policy = policyWith("93.184.216.34", "2606:2800:220:1:248:1893:25c8:1946");

		assertThatCode(() -> policy.requirePublicAddresses(URI.create("https://example.com/menu")))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsAHostnameWhenAnyResultCanReachAPrivateNetwork() throws Exception {
		PublicNetworkAddressPolicy policy = policyWith("93.184.216.34", "10.0.0.5");

		assertThatThrownBy(() -> policy.requirePublicAddresses(URI.create("https://example.com/menu")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("public addresses");
	}

	@Test
	void rejectsNonPublicIpv4AndIpv6Ranges() throws Exception {
		for (String address : List.of(
				"100.64.0.1",
				"169.254.1.1",
				"192.0.2.1",
				"198.18.0.1",
				"203.0.113.1",
				"fc00::1",
				"2001:db8::1")) {
			PublicNetworkAddressPolicy policy = policyWith(address);
			assertThatThrownBy(() -> policy.requirePublicAddresses(URI.create("https://example.com/menu")))
					.as("address %s", address)
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void rejectsUnresolvableHosts() {
		HostnameResolver resolver = hostname -> {
			throw new UnknownHostException(hostname);
		};
		PublicNetworkAddressPolicy policy = new PublicNetworkAddressPolicy(resolver);

		assertThatThrownBy(() -> policy.requirePublicAddresses(URI.create("https://missing.example/menu")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("could not be resolved");
	}

	private PublicNetworkAddressPolicy policyWith(String... addresses) throws UnknownHostException {
		List<InetAddress> resolved = List.of(addresses).stream()
				.map(this::address)
				.toList();
		return new PublicNetworkAddressPolicy(hostname -> resolved);
	}

	private InetAddress address(String value) {
		try {
			return InetAddress.getByName(value);
		} catch (UnknownHostException exception) {
			throw new IllegalArgumentException(exception);
		}
	}
}
