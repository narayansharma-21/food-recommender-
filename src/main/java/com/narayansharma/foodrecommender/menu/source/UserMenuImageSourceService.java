package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.menu.source.SizeLimitedInputStream.MenuImageTooLargeException;
import com.narayansharma.foodrecommender.platform.storage.ObjectStorage;
import com.narayansharma.foodrecommender.platform.storage.StoredObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UserMenuImageSourceService {
	private static final Logger log = LoggerFactory.getLogger(UserMenuImageSourceService.class);
	private static final String STORAGE_NAMESPACE = "menu-images";
	private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of("image/jpeg", "image/png");
	private static final byte[] JPEG_SIGNATURE = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
	private static final byte[] PNG_SIGNATURE = {
			(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectStorage objectStorage;
	private final Clock clock;
	private final long maximumBytes;

	public UserMenuImageSourceService(
			JdbcTemplate jdbcTemplate,
			ObjectStorage objectStorage,
			Clock clock,
			@Value("${menu.upload.max-bytes:10485760}") long maximumBytes) {
		if (maximumBytes < PNG_SIGNATURE.length) {
			throw new IllegalArgumentException("Menu image size limit is too small");
		}
		this.jdbcTemplate = jdbcTemplate;
		this.objectStorage = objectStorage;
		this.clock = clock;
		this.maximumBytes = maximumBytes;
	}

	@Transactional
	public UploadedMenuImageSource upload(
			UUID menuId,
			String requesterReference,
			String mediaType,
			InputStream content) {
		validateRequest(menuId, requesterReference, mediaType, content);
		requireMenu(menuId);
		requireTransactionSynchronization();
		String normalizedMediaType = mediaType.toLowerCase(Locale.ROOT);
		StoredObject storedObject = storeValidatedImage(normalizedMediaType, content);
		deleteObjectIfTransactionRollsBack(storedObject.key());

		UUID sourceId = UUID.randomUUID();
		Instant now = Instant.now(clock);
		jdbcTemplate.update("""
				INSERT INTO menu_sources (
				    id, menu_id, source_type, raw_object_key, submitted_by_reference,
				    media_type, size_bytes, created_at, updated_at
				) VALUES (?, ?, 'USER_IMAGE', ?, ?, ?, ?, ?, ?)
				""",
				sourceId,
				menuId,
				storedObject.key(),
				requesterReference,
				normalizedMediaType,
				storedObject.size(),
				Timestamp.from(now),
				Timestamp.from(now));
		return new UploadedMenuImageSource(
				sourceId,
				storedObject.key(),
				normalizedMediaType,
				storedObject.size(),
				storedObject.sha256());
	}

	private StoredObject storeValidatedImage(String mediaType, InputStream content) {
		try (PushbackInputStream input = new PushbackInputStream(content, PNG_SIGNATURE.length)) {
			byte[] header = input.readNBytes(PNG_SIGNATURE.length);
			if (!hasExpectedSignature(mediaType, header)) {
				throw new IllegalArgumentException("Uploaded menu content does not match its image type");
			}
			input.unread(header);
			return objectStorage.store(STORAGE_NAMESPACE, new SizeLimitedInputStream(input, maximumBytes));
		} catch (MenuImageTooLargeException exception) {
			throw new IllegalArgumentException("Uploaded menu image exceeds the size limit");
		} catch (IOException exception) {
			throw new IllegalStateException("Uploaded menu image could not be stored", exception);
		}
	}

	private boolean hasExpectedSignature(String mediaType, byte[] header) {
		byte[] signature = "image/jpeg".equals(mediaType) ? JPEG_SIGNATURE : PNG_SIGNATURE;
		return header.length >= signature.length
				&& Arrays.equals(signature, Arrays.copyOf(header, signature.length));
	}

	private void deleteObjectIfTransactionRollsBack(String objectKey) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					try {
						objectStorage.delete(objectKey);
					} catch (IOException | RuntimeException exception) {
						log.warn("Could not remove rolled-back menu image object key={}", objectKey, exception);
					}
				}
			}
		});
	}

	private void requireTransactionSynchronization() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("Menu image upload requires an active transaction");
		}
	}

	private void validateRequest(
			UUID menuId,
			String requesterReference,
			String mediaType,
			InputStream content) {
		if (menuId == null) {
			throw new IllegalArgumentException("Menu ID is required");
		}
		if (requesterReference == null || requesterReference.isBlank() || requesterReference.length() > 200) {
			throw new IllegalArgumentException("Signed-in requester reference is required");
		}
		if (mediaType == null || !ALLOWED_MEDIA_TYPES.contains(mediaType.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("Menu image must be JPEG or PNG");
		}
		if (content == null) {
			throw new IllegalArgumentException("Menu image content is required");
		}
	}

	private void requireMenu(UUID menuId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM menus WHERE id = ?",
				Integer.class,
				menuId);
		if (count == null || count != 1) {
			throw new IllegalArgumentException("Unknown menu: " + menuId);
		}
	}
}
