package com.narayansharma.foodrecommender.catalog.matching;

import com.narayansharma.foodrecommender.catalog.discovery.ExternalRestaurantId;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantAddress;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCandidate;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCoordinates;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantTextNormalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMatcher {
	private static final double LIKELY_THRESHOLD = 0.75;
	private static final double REVIEW_THRESHOLD = 0.50;
	private static final double AMBIGUOUS_MARGIN = 0.10;

	public RestaurantMatchResult match(
			RestaurantCandidate candidate,
			List<KnownRestaurantLocation> knownLocations) {
		if (candidate == null || knownLocations == null) {
			throw new IllegalArgumentException("Candidate and known locations are required");
		}

		List<ScoredLocation> scored = knownLocations.stream()
				.map(location -> score(candidate, location))
				.sorted((left, right) -> Double.compare(right.score(), left.score()))
				.toList();
		if (scored.isEmpty() || scored.getFirst().score() < REVIEW_THRESHOLD) {
			return RestaurantMatchResult.none();
		}

		ScoredLocation best = scored.getFirst();
		if (best.externalIdMatch()) {
			return result(RestaurantMatchLevel.EXACT, best);
		}
		boolean ambiguous = scored.size() > 1
				&& scored.get(1).score() >= REVIEW_THRESHOLD
				&& best.score() - scored.get(1).score() < AMBIGUOUS_MARGIN;
		RestaurantMatchLevel level = best.score() >= LIKELY_THRESHOLD && !ambiguous
				? RestaurantMatchLevel.LIKELY
				: RestaurantMatchLevel.REVIEW;
		return result(level, best);
	}

	private RestaurantMatchResult result(RestaurantMatchLevel level, ScoredLocation scored) {
		return new RestaurantMatchResult(level, scored.location(), scored.score(), scored.reasons());
	}

	private ScoredLocation score(RestaurantCandidate candidate, KnownRestaurantLocation location) {
		List<String> reasons = new ArrayList<>();
		if (location.externalIds().contains(candidate.externalId())) {
			reasons.add("same external ID");
			return new ScoredLocation(location, 1, reasons, true);
		}

		double score = 0;
		double nameSimilarity = tokenSimilarity(candidate.displayName(), location.displayName());
		if (nameSimilarity >= 0.5) {
			score += 0.40 * nameSimilarity;
			reasons.add("similar name");
		}
		if (samePhone(candidate.phone(), location.phone())) {
			score += 0.25;
			reasons.add("same phone");
		}
		if (sameAddress(candidate.address(), location.address())) {
			score += 0.25;
			reasons.add("same address");
		}
		if (samePostalCode(candidate.address(), location.address())) {
			score += 0.05;
			reasons.add("same postal code");
		}
		double distance = distanceMeters(candidate.coordinates(), location.coordinates());
		if (distance <= 75) {
			score += 0.25;
			reasons.add("within 75 meters");
		} else if (distance <= 250) {
			score += 0.15;
			reasons.add("within 250 meters");
		} else if (distance <= 1_000) {
			score += 0.05;
			reasons.add("within 1 kilometer");
		}
		return new ScoredLocation(location, Math.min(score, 1), reasons, false);
	}

	private double tokenSimilarity(String left, String right) {
		Set<String> leftTokens = tokens(left);
		Set<String> rightTokens = tokens(right);
		if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
			return 0;
		}
		Set<String> intersection = new HashSet<>(leftTokens);
		intersection.retainAll(rightTokens);
		return 2.0 * intersection.size() / (leftTokens.size() + rightTokens.size());
	}

	private Set<String> tokens(String value) {
		String normalized = RestaurantTextNormalizer.normalize(value);
		return normalized.isEmpty() ? Set.of() : new HashSet<>(List.of(normalized.split(" ")));
	}

	private boolean samePhone(String left, String right) {
		String normalizedLeft = normalizePhone(left);
		String normalizedRight = normalizePhone(right);
		return normalizedLeft.length() >= 7 && normalizedLeft.equals(normalizedRight);
	}

	private String normalizePhone(String phone) {
		if (phone == null) {
			return "";
		}
		String digits = phone.replaceAll("[^0-9]", "");
		return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
	}

	private boolean sameAddress(RestaurantAddress left, RestaurantAddress right) {
		if (left == null || right == null) {
			return false;
		}
		String leftStreet = RestaurantTextNormalizer.normalize(left.addressLine1());
		String rightStreet = RestaurantTextNormalizer.normalize(right.addressLine1());
		return !leftStreet.isEmpty()
				&& leftStreet.equals(rightStreet)
				&& normalizedEquals(left.city(), right.city())
				&& normalizedEquals(left.region(), right.region());
	}

	private boolean samePostalCode(RestaurantAddress left, RestaurantAddress right) {
		return left != null && right != null && normalizedEquals(left.postalCode(), right.postalCode());
	}

	private boolean normalizedEquals(String left, String right) {
		String normalizedLeft = RestaurantTextNormalizer.normalize(left);
		return !normalizedLeft.isEmpty()
				&& normalizedLeft.equals(RestaurantTextNormalizer.normalize(right));
	}

	private double distanceMeters(RestaurantCoordinates left, RestaurantCoordinates right) {
		if (left == null || right == null) {
			return Double.POSITIVE_INFINITY;
		}
		double latitude1 = Math.toRadians(left.latitude().doubleValue());
		double latitude2 = Math.toRadians(right.latitude().doubleValue());
		double latitudeDifference = latitude2 - latitude1;
		double longitudeDifference = Math.toRadians(
				right.longitude().doubleValue() - left.longitude().doubleValue());
		double haversine = Math.pow(Math.sin(latitudeDifference / 2), 2)
				+ Math.cos(latitude1) * Math.cos(latitude2)
				* Math.pow(Math.sin(longitudeDifference / 2), 2);
		haversine = Math.min(1, haversine);
		return 6_371_000 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
	}

	private record ScoredLocation(
			KnownRestaurantLocation location,
			double score,
			List<String> reasons,
			boolean externalIdMatch) {
	}
}
