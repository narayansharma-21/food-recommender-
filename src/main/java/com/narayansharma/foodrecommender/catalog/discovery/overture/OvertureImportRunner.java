package com.narayansharma.foodrecommender.catalog.discovery.overture;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catalog.overture.import-on-startup", havingValue = "true")
class OvertureImportRunner implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(OvertureImportRunner.class);
	private final OvertureRestaurantImporter importer;
	private final Resource snapshot;

	OvertureImportRunner(
			OvertureRestaurantImporter importer,
			ResourceLoader resourceLoader,
			@Value("${catalog.overture.snapshot}") String snapshotLocation) {
		this.importer = importer;
		this.snapshot = resourceLoader.getResource(snapshotLocation);
	}

	@Override
	public void run(ApplicationArguments arguments) throws Exception {
		try (InputStream input = snapshot.getInputStream()) {
			int imported = importer.importSnapshot(input);
			LOGGER.info("Imported {} Overture restaurant source records", imported);
		}
	}
}
