package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.menu.version.CapturedMenuVersion;
import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class OfficialMenuFetchService {
	private static final Logger log = LoggerFactory.getLogger(OfficialMenuFetchService.class);
	private static final String STORAGE_NAMESPACE = "official-menus";

	private final JdbcTemplate jdbcTemplate;
	private final OfficialMenuDocumentFetcher documentFetcher;
	private final ObjectStorage objectStorage;
	private final OfficialMenuFetchPersistenceService persistenceService;
	private final Clock clock;

	OfficialMenuFetchService(
			JdbcTemplate jdbcTemplate,
			OfficialMenuDocumentFetcher documentFetcher,
			ObjectStorage objectStorage,
			OfficialMenuFetchPersistenceService persistenceService,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.documentFetcher = documentFetcher;
		this.objectStorage = objectStorage;
		this.persistenceService = persistenceService;
		this.clock = clock;
	}

	CapturedMenuVersion fetch(UUID sourceId) {
		OfficialSource source = source(sourceId);
		FetchedOfficialMenu document = documentFetcher.fetch(source.sourceType(), source.originUrl());
		StoredObject storedObject = store(document);
		try {
			CapturedMenuVersion version = persistenceService.capture(
					sourceId, storedObject, document.mediaType(), Instant.now(clock));
			if (!version.createdNewVersion()) {
				delete(storedObject.key());
			}
			return version;
		} catch (RuntimeException exception) {
			delete(storedObject.key());
			throw exception;
		}
	}

	private OfficialSource source(UUID sourceId) {
		if (sourceId == null) {
			throw new IllegalArgumentException("Official menu source ID is required");
		}
		List<OfficialSource> sources = jdbcTemplate.query("""
				SELECT source_type, origin_url
				FROM menu_sources
				WHERE id = ?
				""", (resultSet, rowNumber) -> new OfficialSource(
				MenuSourceType.valueOf(resultSet.getString("source_type")),
				resultSet.getString("origin_url")), sourceId);
		if (sources.isEmpty()) {
			throw new IllegalArgumentException("Unknown official menu source: " + sourceId);
		}
		OfficialSource source = sources.getFirst();
		if (source.sourceType() != MenuSourceType.OFFICIAL_HTML
				&& source.sourceType() != MenuSourceType.OFFICIAL_PDF) {
			throw new IllegalArgumentException("Menu source is not an official URL");
		}
		if (source.originUrlValue() == null) {
			throw new IllegalStateException("Official menu source has no URL");
		}
		return source;
	}

	private StoredObject store(FetchedOfficialMenu document) {
		try {
			return objectStorage.store(
					STORAGE_NAMESPACE,
					new ByteArrayInputStream(document.content()));
		} catch (IOException exception) {
			throw new OfficialMenuFetchException("Official menu could not be stored", exception);
		}
	}

	private void delete(String objectKey) {
		try {
			objectStorage.delete(objectKey);
		} catch (IOException | RuntimeException exception) {
			log.warn("Could not remove unused official menu object key={}", objectKey, exception);
		}
	}

	private record OfficialSource(MenuSourceType sourceType, String originUrlValue) {
		private URI originUrl() {
			return URI.create(originUrlValue);
		}
	}
}
