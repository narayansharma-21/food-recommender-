package com.narayansharma.foodrecommender.menu.extraction.job;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrDocument;
import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class StoredMenuVersionDocumentLoader implements MenuVersionDocumentLoader {
	private final JdbcTemplate jdbcTemplate;
	private final ObjectStorage objectStorage;
	private final int maxBytes;

	StoredMenuVersionDocumentLoader(
			JdbcTemplate jdbcTemplate,
			ObjectStorage objectStorage,
			@Value("${menu.upload.max-bytes:10485760}") int maxBytes) {
		if (maxBytes < 1 || maxBytes > 50 * 1024 * 1024) {
			throw new IllegalArgumentException("Menu extraction byte limit is invalid");
		}
		this.jdbcTemplate = jdbcTemplate;
		this.objectStorage = objectStorage;
		this.maxBytes = maxBytes;
	}

	@Override
	public OcrDocument load(UUID menuVersionId) {
		if (menuVersionId == null) {
			throw new IllegalArgumentException("Menu version is required");
		}
		List<RawDocument> documents = jdbcTemplate.query("""
				SELECT raw_object_key, media_type, size_bytes
				FROM menu_versions
				WHERE id = ?
				""",
				(resultSet, rowNumber) -> new RawDocument(
						resultSet.getString("raw_object_key"),
						resultSet.getString("media_type"),
						resultSet.getLong("size_bytes")),
				menuVersionId);
		RawDocument document = documents.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown menu version: " + menuVersionId));
		if (document.objectKey() == null || document.mediaType() == null || document.sizeBytes() < 1) {
			throw new IllegalStateException("Menu version has no captured document");
		}
		if (document.sizeBytes() > maxBytes) {
			throw new IllegalStateException("Captured menu exceeds the extraction size limit");
		}
		try (InputStream input = objectStorage.load(document.objectKey())) {
			byte[] content = input.readNBytes(maxBytes + 1);
			if (content.length != document.sizeBytes()) {
				throw new IllegalStateException("Captured menu size does not match its metadata");
			}
			return new OcrDocument(document.mediaType(), content);
		} catch (IOException exception) {
			throw new IllegalStateException("Captured menu could not be loaded", exception);
		}
	}

	private record RawDocument(String objectKey, String mediaType, long sizeBytes) {
	}
}
