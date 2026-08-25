package com.narayansharma.foodrecommender.platform.jobs;

public interface BackgroundJobHandler {
	String jobType();

	void handle(String payload);
}
