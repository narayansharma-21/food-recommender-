package com.narayansharma.foodrecommender.menu.source;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class OfficialMenuDocumentFetcher {
	private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);
	private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

	private final OfficialMenuUrlPolicy urlPolicy;
	private final PublicNetworkAddressPolicy addressPolicy;
	private final MenuHttpTransport transport;
	private final Duration requestTimeout;
	private final int maximumBytes;
	private final int maximumRedirects;

	OfficialMenuDocumentFetcher(
			OfficialMenuUrlPolicy urlPolicy,
			PublicNetworkAddressPolicy addressPolicy,
			MenuHttpTransport transport,
			@Value("${menu.fetch.request-timeout:PT15S}") Duration requestTimeout,
			@Value("${menu.fetch.max-bytes:10485760}") int maximumBytes,
			@Value("${menu.fetch.max-redirects:3}") int maximumRedirects) {
		if (requestTimeout == null || requestTimeout.toMillis() < 1
				|| maximumBytes < PDF_SIGNATURE.length
				|| maximumBytes == Integer.MAX_VALUE
				|| maximumRedirects < 0
				|| maximumRedirects > 10) {
			throw new IllegalArgumentException("Official menu fetch limits are invalid");
		}
		this.urlPolicy = urlPolicy;
		this.addressPolicy = addressPolicy;
		this.transport = transport;
		this.requestTimeout = requestTimeout;
		this.maximumBytes = maximumBytes;
		this.maximumRedirects = maximumRedirects;
	}

	FetchedOfficialMenu fetch(MenuSourceType sourceType, URI initialUrl) {
		validateSourceType(sourceType);
		URI currentUrl = urlPolicy.validateAndNormalize(initialUrl);
		Set<URI> visited = new HashSet<>();
		for (int redirectCount = 0; redirectCount <= maximumRedirects; redirectCount++) {
			if (!visited.add(currentUrl)) {
				throw new OfficialMenuFetchException("Official menu redirect loop was rejected");
			}
			addressPolicy.requirePublicAddresses(currentUrl);
			try (MenuHttpResponse response = transport.get(currentUrl, requestTimeout)) {
				if (REDIRECT_STATUSES.contains(response.statusCode())) {
					if (redirectCount == maximumRedirects) {
						throw new OfficialMenuFetchException("Official menu redirected too many times");
					}
					currentUrl = redirectedUrl(currentUrl, response);
					continue;
				}
				if (response.statusCode() != 200) {
					throw new OfficialMenuFetchException(
							"Official menu server returned HTTP " + response.statusCode());
				}
				addressPolicy.requirePublicAddresses(currentUrl);
				String mediaType = mediaType(response);
				validateMediaType(sourceType, mediaType);
				validateContentLength(response);
				byte[] content = readBody(response);
				if (content.length > maximumBytes) {
					throw new OfficialMenuFetchException("Official menu exceeds the download size limit");
				}
				validateContent(sourceType, content);
				return new FetchedOfficialMenu(currentUrl, mediaType, content);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new OfficialMenuFetchException("Official menu fetch was interrupted", exception);
			} catch (IOException exception) {
				throw new OfficialMenuFetchException("Official menu could not be downloaded", exception);
			}
		}
		throw new IllegalStateException("Official menu redirect handling did not terminate");
	}

	private byte[] readBody(MenuHttpResponse response) throws IOException, InterruptedException {
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<byte[]> read = executor.submit(() -> response.body().readNBytes(maximumBytes + 1));
			try {
				return read.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
			} catch (TimeoutException exception) {
				response.body().close();
				read.cancel(true);
				throw new OfficialMenuFetchException("Official menu download timed out", exception);
			} catch (ExecutionException exception) {
				if (exception.getCause() instanceof IOException ioException) {
					throw ioException;
				}
				throw new OfficialMenuFetchException("Official menu could not be downloaded", exception.getCause());
			}
		}
	}

	private URI redirectedUrl(URI currentUrl, MenuHttpResponse response) {
		String location = response.location()
				.orElseThrow(() -> new OfficialMenuFetchException("Official menu redirect has no location"));
		try {
			return urlPolicy.validateAndNormalize(currentUrl.resolve(location));
		} catch (IllegalArgumentException exception) {
			throw new OfficialMenuFetchException("Official menu redirect URL was rejected", exception);
		}
	}

	private String mediaType(MenuHttpResponse response) {
		String value = response.contentType()
				.orElseThrow(() -> new OfficialMenuFetchException("Official menu response has no content type"));
		return value.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
	}

	private void validateContentLength(MenuHttpResponse response) {
		if (response.contentLength().isEmpty()) {
			return;
		}
		try {
			long contentLength = Long.parseLong(response.contentLength().get().strip());
			if (contentLength < 0 || contentLength > maximumBytes) {
				throw new OfficialMenuFetchException("Official menu exceeds the download size limit");
			}
		} catch (NumberFormatException exception) {
			throw new OfficialMenuFetchException("Official menu content length is invalid", exception);
		}
	}

	private void validateSourceType(MenuSourceType sourceType) {
		if (sourceType != MenuSourceType.OFFICIAL_HTML && sourceType != MenuSourceType.OFFICIAL_PDF) {
			throw new IllegalArgumentException("Official menu source must be HTML or PDF");
		}
	}

	private void validateMediaType(MenuSourceType sourceType, String mediaType) {
		boolean valid = sourceType == MenuSourceType.OFFICIAL_HTML
				? mediaType.equals("text/html") || mediaType.equals("application/xhtml+xml")
				: mediaType.equals("application/pdf");
		if (!valid) {
			throw new OfficialMenuFetchException("Official menu response type does not match its source type");
		}
	}

	private void validateContent(MenuSourceType sourceType, byte[] content) {
		if (content.length == 0) {
			throw new OfficialMenuFetchException("Official menu response was empty");
		}
		if (sourceType == MenuSourceType.OFFICIAL_PDF) {
			if (content.length < PDF_SIGNATURE.length) {
				throw new OfficialMenuFetchException("Official menu response is not a PDF");
			}
			for (int index = 0; index < PDF_SIGNATURE.length; index++) {
				if (content[index] != PDF_SIGNATURE[index]) {
					throw new OfficialMenuFetchException("Official menu response is not a PDF");
				}
			}
		}
	}
}
