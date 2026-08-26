package com.narayansharma.foodrecommender.catalog.discovery.overture;

import com.narayansharma.foodrecommender.catalog.discovery.RestaurantSearchSourceException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OvertureRestaurantImporter {
	static final String SOURCE = "overture";
	private static final int BATCH_SIZE = 500;

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final OvertureFeatureMapper featureMapper;
	private final RestaurantSourceRecordStore recordStore;
	private final int minimumRecords;

	public OvertureRestaurantImporter(
			ObjectMapper objectMapper,
			Clock clock,
			OvertureFeatureMapper featureMapper,
			RestaurantSourceRecordStore recordStore,
			@Value("${catalog.overture.minimum-records:100}") int minimumRecords) {
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.featureMapper = featureMapper;
		this.recordStore = recordStore;
		this.minimumRecords = minimumRecords;
	}

	@Transactional
	public int importSnapshot(InputStream input) {
		if (input == null) {
			throw new IllegalArgumentException("Overture snapshot is required");
		}

		recordStore.deleteSource(SOURCE);
		Instant importedAt = clock.instant();
		List<RestaurantSourceRecord> batch = new ArrayList<>(BATCH_SIZE);
		int imported = 0;

		try (JsonParser parser = objectMapper.createParser(input)) {
			if (!moveToFeatures(parser)) {
				throw new RestaurantSearchSourceException("Overture snapshot has no features array");
			}
			JsonToken token;
			while ((token = parser.nextToken()) == JsonToken.START_OBJECT) {
				JsonNode feature = parser.readValueAsTree();
				RestaurantSourceRecord record = featureMapper.map(feature, importedAt);
				if (record != null) {
					batch.add(record);
					imported++;
				}
				if (batch.size() == BATCH_SIZE) {
					recordStore.insert(SOURCE, batch);
					batch.clear();
				}
			}
			if (token != JsonToken.END_ARRAY) {
				throw new RestaurantSearchSourceException("Overture features array is invalid");
			}
			recordStore.insert(SOURCE, batch);
			if (imported < minimumRecords) {
				throw new RestaurantSearchSourceException(
						"Overture snapshot produced fewer than " + minimumRecords + " restaurant records");
			}
			return imported;
		} catch (IOException exception) {
			throw new RestaurantSearchSourceException("Could not read Overture snapshot", exception);
		}
	}

	private boolean moveToFeatures(JsonParser parser) throws IOException {
		while (parser.nextToken() != null) {
			if (parser.currentToken() == JsonToken.PROPERTY_NAME && "features".equals(parser.currentName())) {
				return parser.nextToken() == JsonToken.START_ARRAY;
			}
		}
		return false;
	}
}
