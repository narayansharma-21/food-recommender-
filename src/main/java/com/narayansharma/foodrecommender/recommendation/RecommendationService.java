package com.narayansharma.foodrecommender.recommendation;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
	static final String ALGORITHM_VERSION = "weighted-rules-v1";
	static final String NO_PROFILE_VERSION = "no-profile-v1";

	private final RecommendationCandidateService candidateService;
	private final RecommendationRankingService rankingService;
	private final RecommendationImpressionStore impressionStore;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public RecommendationService(
			RecommendationCandidateService candidateService,
			RecommendationRankingService rankingService,
			RecommendationImpressionStore impressionStore,
			JdbcTemplate jdbcTemplate,
			Clock clock) {
		this.candidateService = candidateService;
		this.rankingService = rankingService;
		this.impressionStore = impressionStore;
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	public RecommendationResponse recommend(
			UUID userId,
			UUID restaurantId,
			RecommendationRequest request) {
		validate(userId, restaurantId, request);
		RecommendationCandidateSet candidateSet = candidateService.safeCandidateSet(userId, restaurantId);
		List<RankedRecommendation> ranked = rankingService.rank(
				userId, request.mode(), candidateSet.candidates(), request.limit());
		String featureVersion = featureVersion(userId);
		Instant generatedAt = clock.instant();
		impressionStore.save(
				userId,
				restaurantId,
				candidateSet.menuVersionId(),
				request.mode(),
				ALGORITHM_VERSION,
				featureVersion,
				generatedAt,
				ranked);
		return new RecommendationResponse(
				ALGORITHM_VERSION,
				featureVersion,
				candidateSet.menuVersionId(),
				request.mode(),
				generatedAt,
				views(ranked));
	}

	private List<RecommendationItemView> views(List<RankedRecommendation> ranked) {
		return IntStream.range(0, ranked.size())
				.mapToObj(index -> {
					RankedRecommendation result = ranked.get(index);
					return new RecommendationItemView(
							index + 1,
							result.candidate().menuItemId(),
							result.candidate().displayName(),
							result.score().score(),
							result.score().confidence(),
							result.explanations().stream().map(RecommendationExplanation::text).toList());
				})
				.toList();
	}

	private String featureVersion(UUID userId) {
		List<String> versions = jdbcTemplate.queryForList(
				"SELECT calculation_version FROM taste_profiles WHERE user_id = ?",
				String.class,
				userId);
		return versions.isEmpty() ? NO_PROFILE_VERSION : versions.getFirst();
	}

	private void validate(UUID userId, UUID restaurantId, RecommendationRequest request) {
		if (userId == null || restaurantId == null || request == null || request.mode() == null) {
			throw new IllegalArgumentException("Recommendation request is invalid");
		}
		if (request.limit() < 1 || request.limit() > 25) {
			throw new IllegalArgumentException("Recommendation limit must be between 1 and 25");
		}
	}
}
