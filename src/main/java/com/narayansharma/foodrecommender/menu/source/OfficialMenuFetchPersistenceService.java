package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.menu.version.CapturedMenuVersion;
import com.narayansharma.foodrecommender.menu.version.MenuVersionCaptureService;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OfficialMenuFetchPersistenceService {
	private final JdbcTemplate jdbcTemplate;
	private final MenuVersionCaptureService versionCaptureService;
	private final Clock clock;

	OfficialMenuFetchPersistenceService(
			JdbcTemplate jdbcTemplate,
			MenuVersionCaptureService versionCaptureService,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.versionCaptureService = versionCaptureService;
		this.clock = clock;
	}

	@Transactional
	CapturedMenuVersion capture(UUID sourceId, StoredObject storedObject, String mediaType, Instant capturedAt) {
		CapturedMenuVersion version = versionCaptureService.capture(
				sourceId, storedObject, mediaType, capturedAt);
		if (version.createdNewVersion()) {
			jdbcTemplate.update("""
					UPDATE menu_sources
					SET raw_object_key = ?, media_type = ?, size_bytes = ?, updated_at = ?
					WHERE id = ?
					""",
					storedObject.key(),
					mediaType,
					storedObject.size(),
					Timestamp.from(clock.instant()),
					sourceId);
		}
		return version;
	}
}
