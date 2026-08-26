package com.narayansharma.foodrecommender.catalog.identifiers;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantExternalIdService {
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public RestaurantExternalIdService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public void attach(ExternalRestaurantId externalId, UUID locationId) {
		if (externalId == null || locationId == null) {
			throw new IllegalArgumentException("External ID and restaurant location are required");
		}
		requireActiveLocation(locationId);

		List<UUID> existingLocations = jdbcTemplate.query("""
				SELECT location_id
				FROM restaurant_external_ids
				WHERE source = ? AND external_id = ?
				""",
				(resultSet, rowNumber) -> resultSet.getObject("location_id", UUID.class),
				externalId.source(),
				externalId.value());
		if (!existingLocations.isEmpty()) {
			UUID resolvedExisting = resolveCurrentLocation(existingLocations.getFirst());
			if (locationId.equals(resolvedExisting)) {
				return;
			}
			throw new IllegalStateException("External restaurant ID is already attached to another location");
		}

		Instant now = Instant.now(clock);
		jdbcTemplate.update("""
				INSERT INTO restaurant_external_ids (
				    source, external_id, location_id, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?)
				""",
				externalId.source(),
				externalId.value(),
				locationId,
				Timestamp.from(now),
				Timestamp.from(now));
	}

	@Transactional(readOnly = true)
	public Optional<ResolvedRestaurantLocation> resolve(ExternalRestaurantId externalId) {
		if (externalId == null) {
			throw new IllegalArgumentException("External restaurant ID is required");
		}
		return jdbcTemplate.query("""
				SELECT resolved.restaurant_id, resolved.id AS location_id
				FROM restaurant_external_ids external_id
				JOIN restaurant_locations original ON original.id = external_id.location_id
				JOIN restaurant_locations resolved
				  ON resolved.id = COALESCE(original.merged_into_id, original.id)
				WHERE external_id.source = ? AND external_id.external_id = ?
				""",
				(resultSet, rowNumber) -> new ResolvedRestaurantLocation(
						externalId,
						resultSet.getObject("restaurant_id", UUID.class),
						resultSet.getObject("location_id", UUID.class)),
				externalId.source(),
				externalId.value()).stream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public List<ExternalRestaurantId> findForLocation(UUID locationId) {
		if (locationId == null) {
			throw new IllegalArgumentException("Restaurant location is required");
		}
		return jdbcTemplate.query("""
				SELECT external_id.source, external_id.external_id
				FROM restaurant_external_ids external_id
				JOIN restaurant_locations original ON original.id = external_id.location_id
				WHERE original.id = ? OR original.merged_into_id = ?
				ORDER BY external_id.source, external_id.external_id
				""",
				(resultSet, rowNumber) -> new ExternalRestaurantId(
						resultSet.getString("source"),
						resultSet.getString("external_id")),
				locationId,
				locationId);
	}

	private void requireActiveLocation(UUID locationId) {
		Integer active = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM restaurant_locations
				WHERE id = ? AND status = 'ACTIVE' AND merged_into_id IS NULL
				""", Integer.class, locationId);
		if (active == null || active != 1) {
			throw new IllegalArgumentException("External ID requires an active restaurant location");
		}
	}

	private UUID resolveCurrentLocation(UUID locationId) {
		return jdbcTemplate.queryForObject("""
				SELECT COALESCE(merged_into_id, id)
				FROM restaurant_locations
				WHERE id = ?
				""", UUID.class, locationId);
	}
}
