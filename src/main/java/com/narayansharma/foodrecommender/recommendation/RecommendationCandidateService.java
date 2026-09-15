package com.narayansharma.foodrecommender.recommendation;

import com.narayansharma.foodrecommender.catalog.dish.safety.AllergyEvidenceAssessment;
import com.narayansharma.foodrecommender.catalog.dish.safety.AllergyEvidencePolicy;
import com.narayansharma.foodrecommender.platform.web.ApiException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationCandidateService {
	private final JdbcTemplate jdbcTemplate;
	private final AllergyEvidencePolicy allergyPolicy;
	private final DietaryRestrictionPolicy dietaryPolicy;

	public RecommendationCandidateService(
			JdbcTemplate jdbcTemplate,
			AllergyEvidencePolicy allergyPolicy,
			DietaryRestrictionPolicy dietaryPolicy) {
		this.jdbcTemplate = jdbcTemplate;
		this.allergyPolicy = allergyPolicy;
		this.dietaryPolicy = dietaryPolicy;
	}

	public List<RecommendationCandidate> safeCandidates(UUID userId, UUID restaurantId) {
		return safeCandidateSet(userId, restaurantId).candidates();
	}

	public RecommendationCandidateSet safeCandidateSet(UUID userId, UUID restaurantId) {
		requireActiveUser(userId);
		UUID menuVersionId = currentMenuVersion(restaurantId);
		Restrictions restrictions = restrictions(userId);
		List<RecommendationCandidate> candidates = menuItems(menuVersionId).stream()
				.filter(candidate -> passesAllergies(candidate.menuItemId(), restrictions.allergies()))
				.filter(candidate -> dietaryPolicy.allows(
						restrictions.dietary(), allergyPolicy.reliablePresentIngredientKeys(candidate.menuItemId())))
				.toList();
		return new RecommendationCandidateSet(menuVersionId, candidates);
	}

	private boolean passesAllergies(UUID menuItemId, Set<String> allergies) {
		return allergies.isEmpty()
				|| allergyPolicy.assess(menuItemId, allergies) == AllergyEvidenceAssessment.CONFIRMED_ABSENT;
	}

	private UUID currentMenuVersion(UUID restaurantId) {
		List<UUID> versions = jdbcTemplate.query("""
				SELECT version.id
				FROM menu_versions version
				JOIN menus menu ON menu.id = version.menu_id
				JOIN restaurant_locations location ON location.id = menu.restaurant_location_id
				WHERE location.restaurant_id = ?
				ORDER BY version.captured_at DESC, version.version_number DESC, version.id
				LIMIT 1
				""", (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), restaurantId);
		if (versions.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "No menu was found for the restaurant.");
		}
		return versions.getFirst();
	}

	private List<RecommendationCandidate> menuItems(UUID menuVersionId) {
		return jdbcTemplate.query("""
				SELECT item.id, item.dish_concept_id, item.display_name
				FROM menu_items item
				JOIN menu_sections section ON section.id = item.menu_section_id
				WHERE section.menu_version_id = ?
				ORDER BY section.display_order, item.display_order, item.id
				""", (resultSet, rowNumber) -> new RecommendationCandidate(
				menuVersionId,
				resultSet.getObject("id", UUID.class),
				resultSet.getObject("dish_concept_id", UUID.class),
				resultSet.getString("display_name")), menuVersionId);
	}

	private Restrictions restrictions(UUID userId) {
		List<Restriction> rows = jdbcTemplate.query("""
				SELECT restriction_type, restriction_key
				FROM user_restrictions
				WHERE user_id = ? AND active = TRUE
				""", (resultSet, rowNumber) -> new Restriction(
				resultSet.getString("restriction_type"),
				resultSet.getString("restriction_key")), userId);
		return new Restrictions(
				keys(rows, "ALLERGY"),
				keys(rows, "DIETARY"));
	}

	private Set<String> keys(List<Restriction> restrictions, String type) {
		return restrictions.stream()
				.filter(restriction -> type.equals(restriction.type()))
				.map(Restriction::key)
				.collect(Collectors.toUnmodifiableSet());
	}

	private void requireActiveUser(UUID userId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE id = ? AND status = 'ACTIVE'", Integer.class, userId);
		if (count == null || count == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "The active user was not found.");
		}
	}

	private record Restriction(String type, String key) {
	}

	private record Restrictions(Set<String> allergies, Set<String> dietary) {
	}
}
