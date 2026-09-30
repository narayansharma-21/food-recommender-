package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OfficialMenuDocumentFetcherTest {
	private static final URI START_URL = URI.create("https://menus.example.com/dinner");

	@Test
	void fetchesABoundedHtmlMenu() throws Exception {
		FakeTransport transport = new FakeTransport(response(
				200, null, "text/html; charset=utf-8", "18", "<html>menu</html>"));
		OfficialMenuDocumentFetcher fetcher = fetcher(transport, publicAddresses());

		FetchedOfficialMenu menu = fetcher.fetch(MenuSourceType.OFFICIAL_HTML, START_URL);

		assertThat(menu.finalUrl()).isEqualTo(START_URL);
		assertThat(menu.mediaType()).isEqualTo("text/html");
		assertThat(new String(menu.content(), StandardCharsets.UTF_8)).isEqualTo("<html>menu</html>");
	}

	@Test
	void followsOnlyValidatedRedirects() throws Exception {
		FakeTransport transport = new FakeTransport(
				response(302, "https://cdn.example.com/menu.pdf", null, null, ""),
				response(200, null, "application/pdf", null, "%PDF-menu"));
		OfficialMenuDocumentFetcher fetcher = fetcher(transport, publicAddresses());

		FetchedOfficialMenu menu = fetcher.fetch(MenuSourceType.OFFICIAL_PDF, START_URL);

		assertThat(menu.finalUrl()).isEqualTo(URI.create("https://cdn.example.com/menu.pdf"));
		assertThat(transport.requestedUrls).containsExactly(
				START_URL,
				URI.create("https://cdn.example.com/menu.pdf"));
	}

	@Test
	void rejectsARedirectThatResolvesToAPrivateAddress() throws Exception {
		FakeTransport transport = new FakeTransport(
				response(302, "https://private.example.com/menu", null, null, ""));
		HostnameResolver resolver = hostname -> List.of(InetAddress.getByName(
				"private.example.com".equals(hostname) ? "127.0.0.1" : "93.184.216.34"));
		OfficialMenuDocumentFetcher fetcher = fetcher(transport, resolver);

		assertThatThrownBy(() -> fetcher.fetch(MenuSourceType.OFFICIAL_HTML, START_URL))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("public addresses");
		assertThat(transport.requestedUrls).containsExactly(START_URL);
	}

	@Test
	void rejectsOversizedOrMismatchedResponses() throws Exception {
		OfficialMenuDocumentFetcher oversized = fetcher(
				new FakeTransport(response(200, null, "text/html", "101", "small")),
				publicAddresses());
		OfficialMenuDocumentFetcher oversizedStream = fetcher(
				new FakeTransport(response(200, null, "text/html", null, "x".repeat(101))),
				publicAddresses());
		OfficialMenuDocumentFetcher mismatched = fetcher(
				new FakeTransport(response(200, null, "image/jpeg", null, "image")),
				publicAddresses());

		assertThatThrownBy(() -> oversized.fetch(MenuSourceType.OFFICIAL_HTML, START_URL))
				.isInstanceOf(OfficialMenuFetchException.class)
				.hasMessageContaining("size limit");
		assertThatThrownBy(() -> oversizedStream.fetch(MenuSourceType.OFFICIAL_HTML, START_URL))
				.isInstanceOf(OfficialMenuFetchException.class)
				.hasMessageContaining("size limit");
		assertThatThrownBy(() -> mismatched.fetch(MenuSourceType.OFFICIAL_HTML, START_URL))
				.isInstanceOf(OfficialMenuFetchException.class)
				.hasMessageContaining("source type");
	}

	@Test
	void verifiesPdfContentInsteadOfTrustingTheHeader() throws Exception {
		OfficialMenuDocumentFetcher fetcher = fetcher(
				new FakeTransport(response(200, null, "application/pdf", null, "not-a-pdf")),
				publicAddresses());

		assertThatThrownBy(() -> fetcher.fetch(MenuSourceType.OFFICIAL_PDF, START_URL))
				.isInstanceOf(OfficialMenuFetchException.class)
				.hasMessageContaining("not a PDF");
	}

	private OfficialMenuDocumentFetcher fetcher(
			MenuHttpTransport transport,
			HostnameResolver resolver) {
		return new OfficialMenuDocumentFetcher(
				new OfficialMenuUrlPolicy(),
				new PublicNetworkAddressPolicy(resolver),
				transport,
				Duration.ofSeconds(1),
				100,
				3);
	}

	private HostnameResolver publicAddresses() throws Exception {
		return hostname -> List.of(InetAddress.getByName("93.184.216.34"));
	}

	private MenuHttpResponse response(
			int status,
			String location,
			String contentType,
			String contentLength,
			String body) {
		return new MenuHttpResponse(
				status,
				Optional.ofNullable(location),
				Optional.ofNullable(contentType),
				Optional.ofNullable(contentLength),
				new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
	}

	private static final class FakeTransport implements MenuHttpTransport {
		private final ArrayDeque<MenuHttpResponse> responses;
		private final List<URI> requestedUrls = new ArrayList<>();

		private FakeTransport(MenuHttpResponse... responses) {
			this.responses = new ArrayDeque<>(List.of(responses));
		}

		@Override
		public MenuHttpResponse get(URI url, Duration timeout) {
			requestedUrls.add(url);
			return responses.removeFirst();
		}
	}
}
