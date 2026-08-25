package com.narayansharma.foodrecommender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodRecommenderBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodRecommenderBackendApplication.class, args);
	}

}
