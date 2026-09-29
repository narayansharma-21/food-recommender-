package com.narayansharma.foodrecommender.catalog.query;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantCandidate;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchPage;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchQuery;
import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchSource;
import com.narayansharma.foodrecommender.catalog.identifiers.ResolvedRestaurantLocation;
import com.narayansharma.foodrecommender.catalog.identifiers.RestaurantExternalIdService;
import org.springframework.stereotype.Service;

@Service
public class RestaurantSearchService {
	private final RestaurantSearchSource searchSource;
	private final RestaurantExternalIdService externalIdService;

	public RestaurantSearchService(
			RestaurantSearchSource searchSource,
			RestaurantExternalIdService externalIdService) {
		this.searchSource = searchSource;
		this.externalIdService = externalIdService;
	}

	public RestaurantSearchResponse search(RestaurantSearchQuery query) {
		RestaurantSearchPage page = searchSource.search(query);
		return new RestaurantSearchResponse(
				page.restaurants().stream().map(this::result).toList(),
				page.nextCursor());
	}

	private RestaurantSearchResult result(RestaurantCandidate candidate) {
		ResolvedRestaurantLocation resolved = externalIdService.resolve(candidate.externalId()).orElse(null);
		return new RestaurantSearchResult(
				resolved == null ? null : resolved.restaurantId(),
				resolved == null ? null : resolved.locationId(),
				candidate.externalId(),
				candidate.displayName(),
				candidate.address(),
				candidate.coordinates(),
				candidate.phone(),
				candidate.website());
	}
}
