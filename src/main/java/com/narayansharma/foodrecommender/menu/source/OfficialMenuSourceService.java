package com.narayansharma.foodrecommender.menu.source;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialMenuSourceService {
	private final JdbcTemplate jdbcTemplate;
	private final OfficialMenuUrlPolicy urlPolicy;
	private final Clock clock;

	public OfficialMenuSourceService(
			JdbcTemplate jdbcTemplate,
			OfficialMenuUrlPolicy urlPolicy,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.urlPolicy = urlPolicy;
		this.clock = clock;
	}

	@Transactional
	public UUID register(UUID menuId, MenuSourceType sourceType, URI url) {
		if (menuId == null) {
			throw new IllegalArgumentException("Menu ID is required");
		}
		if (sourceType != MenuSourceType.OFFICIAL_HTML && sourceType != MenuSourceType.OFFICIAL_PDF) {
			throw new IllegalArgumentException("Official menu source must be HTML or PDF");
		}
		requireMenu(menuId);
		String normalizedUrl = urlPolicy.validateAndNormalize(url).toASCIIString();
		List<UUID> existing = jdbcTemplate.query("""
				SELECT id FROM menu_sources
				WHERE menu_id = ? AND source_type = ? AND origin_url = ?
				""",
				(resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
				menuId,
				sourceType.name(),
				normalizedUrl);
		if (!existing.isEmpty()) {
			return existing.getFirst();
		}

		UUID sourceId = UUID.randomUUID();
		Instant now = Instant.now(clock);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, origin_url, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				sourceId,
				menuId,
				sourceType.name(),
				normalizedUrl,
				Timestamp.from(now),
				Timestamp.from(now));
		return sourceId;
	}

	private void requireMenu(UUID menuId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menus WHERE id = ?",
				Integer.class,
				menuId);
		if (count == null || count != 1) {
			throw new IllegalArgumentException("Unknown menu: " + menuId);
		}
	}
}
