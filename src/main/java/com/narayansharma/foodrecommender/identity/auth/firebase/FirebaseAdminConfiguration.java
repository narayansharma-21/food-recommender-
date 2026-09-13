package com.narayansharma.foodrecommender.identity.auth.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "identity.firebase", name = "enabled", havingValue = "true")
class FirebaseAdminConfiguration {
	@Bean(destroyMethod = "delete")
	FirebaseApp firebaseApp(@Value("${identity.firebase.project-id}") String projectId) throws IOException {
		if (projectId == null || projectId.isBlank() || projectId.length() > 100) {
			throw new IllegalArgumentException("Firebase project ID is invalid");
		}
		FirebaseOptions options = FirebaseOptions.builder()
				.setProjectId(projectId)
				.setCredentials(GoogleCredentials.getApplicationDefault())
				.build();
		return FirebaseApp.initializeApp(options, "food-recommender");
	}

	@Bean
	FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
		return FirebaseAuth.getInstance(firebaseApp);
	}
}
