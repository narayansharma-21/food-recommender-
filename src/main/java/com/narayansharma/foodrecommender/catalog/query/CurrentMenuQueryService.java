package com.narayansharma.foodrecommender.catalog.query;

import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentMenuQueryService {
	private final JdbcTemplate jdbcTemplate;

	public CurrentMenuQueryService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public CurrentMenuView get(UUID restaurantId) {
		if (restaurantId == null) {
			throw new IllegalArgumentException("Restaurant ID is required");
		}
		CurrentMenu menu = findCurrentMenu(restaurantId);
		List<SectionRow> sectionRows = sections(menu.menuVersionId());
		List<ItemRow> itemRows = items(menu.menuVersionId());
		Map<UUID, List<MenuModifierView>> modifiersByItem = modifiers(menu.menuVersionId()).stream()
				.collect(Collectors.groupingBy(ModifierRow::menuItemId, Collectors.mapping(
						ModifierRow::view, Collectors.toList())));
		Map<UUID, List<MenuItemView>> itemsBySection = itemRows.stream()
				.collect(Collectors.groupingBy(ItemRow::sectionId, Collectors.mapping(item -> new MenuItemView(
						item.id(),
						item.dishConceptId(),
						item.displayName(),
						item.description(),
						item.priceAmount(),
						item.priceCurrency(),
						modifiersByItem.getOrDefault(item.id(), List.of())), Collectors.toList())));
		List<MenuSectionView> sectionViews = sectionRows.stream()
				.map(section -> new MenuSectionView(
						section.id(),
						section.displayName(),
						itemsBySection.getOrDefault(section.id(), List.of())))
				.toList();
		return new CurrentMenuView(
				menu.menuId(),
				menu.menuVersionId(),
				menu.locationId(),
				menu.displayName(),
				menu.versionNumber(),
				menu.capturedAt(),
				menu.ready() ? "READY" : "PROCESSING",
				menu.sourceType(),
				parseUrl(menu.sourceUrl()),
				sectionViews);
	}

	private CurrentMenu findCurrentMenu(UUID restaurantId) {
		return jdbcTemplate.query("""
				SELECT menu.id AS menu_id, version.id AS menu_version_id,
				       location.id AS location_id, menu.display_name,
				       version.version_number, version.captured_at,
				       source.source_type, source.origin_url,
				       EXISTS (
				           SELECT 1 FROM menu_extractions extraction
				           WHERE extraction.menu_version_id = version.id
				       ) AS ready
				FROM restaurants requested
				JOIN restaurant_locations location
				  ON location.restaurant_id = COALESCE(requested.merged_into_id, requested.id)
				 AND location.status = 'ACTIVE'
				 AND location.merged_into_id IS NULL
				JOIN menus menu ON menu.restaurant_location_id = location.id
				JOIN menu_versions version ON version.menu_id = menu.id
				JOIN menu_sources source ON source.id = version.source_id
				WHERE requested.id = ?
				ORDER BY version.captured_at DESC, version.version_number DESC, version.id
				LIMIT 1
				""", (resultSet, rowNumber) -> new CurrentMenu(
				resultSet.getObject("menu_id", UUID.class),
				resultSet.getObject("menu_version_id", UUID.class),
				resultSet.getObject("location_id", UUID.class),
				resultSet.getString("display_name"),
				resultSet.getInt("version_number"),
				resultSet.getTimestamp("captured_at").toInstant(),
				resultSet.getBoolean("ready"),
				resultSet.getString("source_type"),
				resultSet.getString("origin_url")), restaurantId).stream()
				.findFirst()
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"MENU_NOT_FOUND",
						"No menu was found for the restaurant."));
	}

	private List<SectionRow> sections(UUID menuVersionId) {
		return jdbcTemplate.query("""
				SELECT id, display_name
				FROM menu_sections
				WHERE menu_version_id = ?
				ORDER BY display_order, id
				""", (resultSet, rowNumber) -> new SectionRow(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("display_name")), menuVersionId);
	}

	private List<ItemRow> items(UUID menuVersionId) {
		return jdbcTemplate.query("""
				SELECT item.id, item.menu_section_id, item.dish_concept_id,
				       item.display_name, item.description, item.price_amount, item.price_currency
				FROM menu_items item
				JOIN menu_sections section ON section.id = item.menu_section_id
				WHERE section.menu_version_id = ?
				ORDER BY section.display_order, item.display_order, item.id
				""", (resultSet, rowNumber) -> new ItemRow(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("menu_section_id", UUID.class),
				resultSet.getObject("dish_concept_id", UUID.class),
				resultSet.getString("display_name"),
				resultSet.getString("description"),
				resultSet.getBigDecimal("price_amount"),
				resultSet.getString("price_currency")), menuVersionId);
	}

	private List<ModifierRow> modifiers(UUID menuVersionId) {
		return jdbcTemplate.query("""
				SELECT modifier.id, modifier.menu_item_id, modifier.display_name, modifier.price_amount
				FROM menu_item_modifiers modifier
				JOIN menu_items item ON item.id = modifier.menu_item_id
				JOIN menu_sections section ON section.id = item.menu_section_id
				WHERE section.menu_version_id = ?
				ORDER BY section.display_order, item.display_order, modifier.display_order, modifier.id
				""", (resultSet, rowNumber) -> new ModifierRow(
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("menu_item_id", UUID.class),
				resultSet.getString("display_name"),
				resultSet.getBigDecimal("price_amount")), menuVersionId);
	}

	private URI parseUrl(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			URI uri = URI.create(value);
			return uri.isAbsolute() && ("http".equalsIgnoreCase(uri.getScheme())
					|| "https".equalsIgnoreCase(uri.getScheme())) ? uri : null;
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private record CurrentMenu(
			UUID menuId,
			UUID menuVersionId,
			UUID locationId,
			String displayName,
			int versionNumber,
			Instant capturedAt,
			boolean ready,
			String sourceType,
			String sourceUrl) {
	}

	private record SectionRow(UUID id, String displayName) {
	}

	private record ItemRow(
			UUID id,
			UUID sectionId,
			UUID dishConceptId,
			String displayName,
			String description,
			BigDecimal priceAmount,
			String priceCurrency) {
	}

	private record ModifierRow(UUID id, UUID menuItemId, String displayName, BigDecimal priceAmount) {
		MenuModifierView view() {
			return new MenuModifierView(id, displayName, priceAmount);
		}
	}
}
