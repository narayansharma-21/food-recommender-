package com.narayansharma.foodrecommender.catalog.discovery.overture;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class RestaurantSourceRecordStore {
	private static final String INSERT_SQL = """
			INSERT INTO restaurant_source_records (
			    source, external_id, display_name, normalized_name, address_line_1,
			    address_line_2, city, region, postal_code, country_code, latitude,
			    longitude, phone, website_url, category, confidence, imported_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private final JdbcTemplate jdbcTemplate;

	RestaurantSourceRecordStore(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	void deleteSource(String source) {
		jdbcTemplate.update("DELETE FROM restaurant_source_records WHERE source = ?", source);
	}

	void insert(String source, List<RestaurantSourceRecord> records) {
		if (records.isEmpty()) {
			return;
		}
		jdbcTemplate.batchUpdate(
				INSERT_SQL,
				records,
				records.size(),
				(statement, record) -> setParameters(statement, source, record));
	}

	private void setParameters(
			PreparedStatement statement,
			String source,
			RestaurantSourceRecord record) throws SQLException {
		statement.setString(1, source);
		statement.setString(2, record.externalId());
		statement.setString(3, record.displayName());
		statement.setString(4, record.normalizedName());
		statement.setString(5, record.addressLine1());
		statement.setString(6, record.addressLine2());
		statement.setString(7, record.city());
		statement.setString(8, record.region());
		statement.setString(9, record.postalCode());
		statement.setString(10, record.countryCode());
		statement.setBigDecimal(11, record.latitude());
		statement.setBigDecimal(12, record.longitude());
		statement.setString(13, record.phone());
		statement.setString(14, record.websiteUrl());
		statement.setString(15, record.category());
		statement.setBigDecimal(16, record.confidence());
		statement.setTimestamp(17, Timestamp.from(record.importedAt()));
	}
}
