package com.narayansharma.foodrecommender.menu.catalog;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuCatalogService {
	private static final Pattern MENU_KEY = Pattern.compile("[a-z0-9][a-z0-9_-]{0,49}");

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public MenuCatalogService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public MenuDefinition findOrCreate(
			UUID locationId,
			String menuKey,
			String displayName) {
		validate(locationId, menuKey, displayName);
		lockActiveLocation(locationId);
		List<MenuDefinition> existing = jdbcTemplate.query("""
				SELECT id, display_name
				FROM menus
				WHERE restaurant_location_id = ? AND menu_key = ?
				""", (resultSet, rowNumber) -> new MenuDefinition(
				resultSet.getObject("id", UUID.class),
				locationId,
				menuKey,
				resultSet.getString("display_name"),
				false), locationId, menuKey);
		if (!existing.isEmpty()) {
			return existing.getFirst();
		}

		UUID menuId = UUID.randomUUID();
		Instant now = clock.instant();
		jdbcTemplate.update("""
				INSERT INTO menus (
				    id, restaurant_location_id, menu_key, display_name, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?)
				""",
				menuId,
				locationId,
				menuKey,
				displayName.strip(),
				Timestamp.from(now),
				Timestamp.from(now));
		return new MenuDefinition(menuId, locationId, menuKey, displayName.strip(), true);
	}

	private void lockActiveLocation(UUID locationId) {
		List<UUID> locations = jdbcTemplate.query("""
				SELECT id
				FROM restaurant_locations
				WHERE id = ? AND status = 'ACTIVE' AND merged_into_id IS NULL
				FOR UPDATE
				""", (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), locationId);
		if (locations.isEmpty()) {
			throw new ApiException(
					HttpStatus.NOT_FOUND,
					"RESTAURANT_LOCATION_NOT_FOUND",
					"The active restaurant location was not found.");
		}
	}

	private void validate(UUID locationId, String menuKey, String displayName) {
		if (locationId == null) {
			throw new IllegalArgumentException("Restaurant location is required");
		}
		if (menuKey == null || !MENU_KEY.matcher(menuKey).matches()) {
			throw new IllegalArgumentException("Menu key is invalid");
		}
		if (displayName == null || displayName.isBlank() || displayName.strip().length() > 100) {
			throw new IllegalArgumentException("Menu display name is invalid");
		}
	}
}
