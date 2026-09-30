package com.narayansharma.foodrecommender.menu.source;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class SystemHostnameResolver implements HostnameResolver {
	@Override
	public List<InetAddress> resolve(String hostname) throws UnknownHostException {
		return Arrays.asList(InetAddress.getAllByName(hostname));
	}
}
