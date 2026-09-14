package com.narayansharma.foodrecommender.taste;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SaveOnboardingResponseRequest(@Min(1) @Max(5) int preferenceScore) {
}
