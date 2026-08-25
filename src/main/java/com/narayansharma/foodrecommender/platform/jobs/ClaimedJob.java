package com.narayansharma.foodrecommender.platform.jobs;

import java.util.UUID;

public record ClaimedJob(UUID id, String jobType, String payload, int attempt) {
}
