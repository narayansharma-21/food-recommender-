package com.narayansharma.foodrecommender.menu.source;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class PublicNetworkAddressPolicy {
	private final HostnameResolver hostnameResolver;

	PublicNetworkAddressPolicy(HostnameResolver hostnameResolver) {
		this.hostnameResolver = hostnameResolver;
	}

	void requirePublicAddresses(URI url) {
		if (url == null || url.getHost() == null) {
			throw invalid();
		}
		try {
			List<InetAddress> addresses = hostnameResolver.resolve(url.getHost());
			if (addresses.isEmpty() || addresses.stream().anyMatch(this::isBlocked)) {
				throw invalid();
			}
		} catch (UnknownHostException exception) {
			throw new IllegalArgumentException("Official menu hostname could not be resolved", exception);
		}
	}

	private boolean isBlocked(InetAddress address) {
		if (address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress()) {
			return true;
		}
		if (address instanceof Inet4Address) {
			return isBlockedIpv4(address.getAddress());
		}
		if (address instanceof Inet6Address) {
			byte[] bytes = address.getAddress();
			return (bytes[0] & 0xfe) == 0xfc || isPrefix(bytes, new int[] {0x20, 0x01, 0x0d, 0xb8});
		}
		return true;
	}

	private boolean isBlockedIpv4(byte[] bytes) {
		int first = Byte.toUnsignedInt(bytes[0]);
		int second = Byte.toUnsignedInt(bytes[1]);
		int third = Byte.toUnsignedInt(bytes[2]);
		return first == 0
				|| first == 10
				|| (first == 100 && second >= 64 && second <= 127)
				|| first == 127
				|| (first == 169 && second == 254)
				|| (first == 172 && second >= 16 && second <= 31)
				|| (first == 192 && second == 0 && third == 0)
				|| (first == 192 && second == 0 && third == 2)
				|| (first == 192 && second == 168)
				|| (first == 198 && (second == 18 || second == 19))
				|| (first == 198 && second == 51 && third == 100)
				|| (first == 203 && second == 0 && third == 113)
				|| first >= 224;
	}

	private boolean isPrefix(byte[] address, int[] prefix) {
		for (int index = 0; index < prefix.length; index++) {
			if (Byte.toUnsignedInt(address[index]) != prefix[index]) {
				return false;
			}
		}
		return true;
	}

	private IllegalArgumentException invalid() {
		return new IllegalArgumentException("Official menu hostname must resolve only to public addresses");
	}
}
