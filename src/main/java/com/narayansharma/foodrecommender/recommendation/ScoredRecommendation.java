package com.narayansharma.foodrecommender.recommendation;

import java.math.BigDecimal;

public record ScoredRecommendation(BigDecimal score, String confidence) {
}
