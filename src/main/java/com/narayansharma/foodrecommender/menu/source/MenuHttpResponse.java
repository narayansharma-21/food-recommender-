package com.narayansharma.foodrecommender.menu.source;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

record MenuHttpResponse(
		int statusCode,
		Optional<String> location,
		Optional<String> contentType,
		Optional<String> contentLength,
		InputStream body) implements AutoCloseable {
	@Override
	public void close() throws IOException {
		body.close();
	}
}
