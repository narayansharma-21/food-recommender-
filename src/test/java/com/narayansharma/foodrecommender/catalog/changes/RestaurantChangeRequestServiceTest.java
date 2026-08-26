package com.narayansharma.foodrecommender.catalog.changes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantChangeRequestServiceTest {
	private static final UUID RESTAURANT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

	@Autowired
	private RestaurantChangeRequestService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertRestaurant() {
		jdbcTemplate.update("""
				INSERT INTO restaurants (id, display_name, normalized_name, created_at, updated_at)
				VALUES (?, 'Cafe Luna', 'cafe luna', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", RESTAURANT_ID);
		jdbcTemplate.update("""
				INSERT INTO restaurant_locations (
				    id, restaurant_id, address_line_1, city, region, country_code,
				    timezone, created_at, updated_at
				) VALUES (?, ?, '10 Main Street', 'Boston', 'MA', 'US',
				          'America/New_York', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", LOCATION_ID, RESTAURANT_ID);
	}

	@Test
	void submitsACorrectionWithEvidence() {
		UUID requestId = service.submitCorrection(
				LOCATION_ID,
				"user-123",
				RestaurantCorrectionField.HOURS,
				"Open until 10 PM",
				"The official site lists new hours",
				URI.create("https://example.com/hours"));

		assertThat(service.pendingRequests()).singleElement().satisfies(request -> {
			assertThat(request.id()).isEqualTo(requestId);
			assertThat(request.kind()).isEqualTo(RestaurantChangeRequestKind.CORRECTION);
			assertThat(request.correctionField()).isEqualTo(RestaurantCorrectionField.HOURS);
			assertThat(request.evidenceUrl()).hasToString("https://example.com/hours");
		});
	}

	@Test
	void submitsAndAcceptsARemovalRequest() {
		UUID requestId = service.submitRemoval(
				LOCATION_ID, "user-123", "This location permanently closed", null);

		service.accept(requestId, "admin-456", "Confirmed on the official website");

		assertThat(service.pendingRequests()).isEmpty();
		assertThat(value("SELECT status FROM restaurant_change_requests WHERE id = ?", requestId))
				.isEqualTo("ACCEPTED");
		assertThat(value("SELECT reviewer_reference FROM restaurant_change_requests WHERE id = ?", requestId))
				.isEqualTo("admin-456");
	}

	@Test
	void rejectsUnsafeEvidenceUrls() {
		assertThatThrownBy(() -> service.submitRemoval(
				LOCATION_ID,
				"user-123",
				"This location closed",
				URI.create("file:///tmp/evidence.txt")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("HTTP");
		assertThatThrownBy(() -> service.submitRemoval(
				LOCATION_ID,
				"user-123",
				"This location closed",
				URI.create("https:/missing-host")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("HTTP");
	}

	@Test
	void doesNotAllowARequestToBeReviewedTwice() {
		UUID requestId = service.submitRemoval(LOCATION_ID, "user-123", "This location closed", null);
		service.reject(requestId, "admin-456", "The restaurant is still open");

		assertThatThrownBy(() -> service.accept(requestId, "admin-456", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already complete");
	}

	private String value(String sql, UUID id) {
		return jdbcTemplate.queryForObject(sql, String.class, id);
	}
}
