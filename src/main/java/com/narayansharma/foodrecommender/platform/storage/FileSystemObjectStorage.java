package com.narayansharma.foodrecommender.platform.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "platform.storage.provider", havingValue = "filesystem", matchIfMissing = true)
class FileSystemObjectStorage implements ObjectStorage {
	private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9][a-z0-9-]{0,62}");
	private final Path root;

	FileSystemObjectStorage(@Value("${platform.storage.filesystem.root:.local-storage}") String root) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@Override
	public StoredObject store(String namespace, InputStream content) throws IOException {
		if (namespace == null || !VALID_NAMESPACE.matcher(namespace).matches()) {
			throw new IllegalArgumentException("Invalid object namespace");
		}
		if (content == null) {
			throw new IllegalArgumentException("Object content is required");
		}

		Path namespaceDirectory = resolveWithinRoot(namespace);
		Files.createDirectories(namespaceDirectory);
		String objectId = UUID.randomUUID().toString();
		Path target = namespaceDirectory.resolve(objectId);
		Path temporary = Files.createTempFile(namespaceDirectory, objectId, ".tmp");
		MessageDigest digest = sha256Digest();

		try {
			long size;
			try (DigestInputStream input = new DigestInputStream(content, digest);
					OutputStream output = Files.newOutputStream(temporary)) {
				size = input.transferTo(output);
			}
			moveIntoPlace(temporary, target);
			return new StoredObject(
					namespace + "/" + objectId,
					size,
					HexFormat.of().formatHex(digest.digest()));
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private void moveIntoPlace(Path temporary, Path target) throws IOException {
		try {
			Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporary, target);
		}
	}

	@Override
	public InputStream load(String key) throws IOException {
		return Files.newInputStream(resolveWithinRoot(key));
	}

	@Override
	public void delete(String key) throws IOException {
		Files.deleteIfExists(resolveWithinRoot(key));
	}

	private Path resolveWithinRoot(String key) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Object key is required");
		}
		Path resolved = root.resolve(key).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("Object key escapes storage root");
		}
		return resolved;
	}

	private MessageDigest sha256Digest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
