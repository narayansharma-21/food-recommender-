package com.narayansharma.foodrecommender.menu.version;

import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuVersionCaptureService {
	private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
	private static final Pattern MEDIA_TYPE = Pattern.compile("[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+");

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public MenuVersionCaptureService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public CapturedMenuVersion capture(
			UUID sourceId,
			StoredObject rawObject,
			String mediaType,
			Instant capturedAt) {
		validate(sourceId, rawObject, mediaType, capturedAt);
		UUID menuId = findMenuId(sourceId);
		lockMenu(menuId);
		Integer nextVersion = jdbcTemplate.queryForObject("""
				SELECT COALESCE(MAX(version_number), 0) + 1
				FROM menu_versions
				WHERE menu_id = ?
				""", Integer.class, menuId);
		if (nextVersion == null) {
			throw new IllegalStateException("Could not allocate a menu version number");
		}

		UUID versionId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO menu_versions (
				    id, menu_id, source_id, version_number, captured_at, created_at,
				    raw_object_key, content_sha256, media_type, size_bytes
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
				versionId,
				menuId,
				sourceId,
				nextVersion,
				Timestamp.from(capturedAt),
				Timestamp.from(Instant.now(clock)),
				rawObject.key(),
				rawObject.sha256(),
				mediaType,
				rawObject.size());
		return new CapturedMenuVersion(versionId, menuId, sourceId, nextVersion, capturedAt);
	}

	private UUID findMenuId(UUID sourceId) {
		List<UUID> menuIds = jdbcTemplate.query(
				"SELECT menu_id FROM menu_sources WHERE id = ?",
				(resultSet, rowNumber) -> resultSet.getObject("menu_id", UUID.class),
				sourceId);
		return menuIds.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown menu source: " + sourceId));
	}

	private void lockMenu(UUID menuId) {
		jdbcTemplate.queryForObject(
				"SELECT id FROM menus WHERE id = ? FOR UPDATE",
				UUID.class,
				menuId);
	}

	private void validate(UUID sourceId, StoredObject rawObject, String mediaType, Instant capturedAt) {
		if (sourceId == null || rawObject == null || capturedAt == null) {
			throw new IllegalArgumentException("Menu source, raw object, and capture time are required");
		}
		if (rawObject.key() == null || rawObject.key().isBlank() || rawObject.key().length() > 500
				|| rawObject.size() < 1
				|| rawObject.sha256() == null
				|| !SHA256.matcher(rawObject.sha256()).matches()) {
			throw new IllegalArgumentException("Raw menu object metadata is invalid");
		}
		if (mediaType == null || mediaType.length() > 100 || !MEDIA_TYPE.matcher(mediaType).matches()) {
			throw new IllegalArgumentException("Raw menu media type is invalid");
		}
		if (capturedAt.isAfter(Instant.now(clock).plus(5, ChronoUnit.MINUTES))) {
			throw new IllegalArgumentException("Menu capture time cannot be in the future");
		}
	}
}
