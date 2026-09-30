package com.narayansharma.foodrecommender.menu.source;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

interface HostnameResolver {
	List<InetAddress> resolve(String hostname) throws UnknownHostException;
}
