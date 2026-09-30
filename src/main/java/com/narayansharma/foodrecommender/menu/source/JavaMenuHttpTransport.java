package com.narayansharma.foodrecommender.menu.source;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class JavaMenuHttpTransport implements MenuHttpTransport {
	private final HttpClient httpClient;

	JavaMenuHttpTransport(
			@Value("${menu.fetch.connect-timeout:PT5S}") Duration connectTimeout) {
		if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
			throw new IllegalArgumentException("Menu fetch connect timeout must be positive");
		}
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	@Override
	public MenuHttpResponse get(URI url, Duration timeout) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(url)
				.timeout(timeout)
				.header("Accept", "text/html,application/xhtml+xml,application/pdf;q=0.9")
				.header("User-Agent", "FoodRecommenderMenuFetcher/1.0")
				.GET()
				.build();
		HttpResponse<java.io.InputStream> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofInputStream());
		return new MenuHttpResponse(
				response.statusCode(),
				response.headers().firstValue("location"),
				response.headers().firstValue("content-type"),
				response.headers().firstValue("content-length"),
				response.body());
	}
}
