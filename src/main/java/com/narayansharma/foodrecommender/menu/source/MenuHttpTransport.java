package com.narayansharma.foodrecommender.menu.source;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

interface MenuHttpTransport {
	MenuHttpResponse get(URI url, Duration timeout) throws IOException, InterruptedException;
}
