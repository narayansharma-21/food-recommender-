package com.narayansharma.foodrecommender.platform.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemObjectStorageTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void storesLoadsAndDeletesObject() throws Exception {
		FileSystemObjectStorage storage = new FileSystemObjectStorage(temporaryDirectory.toString());
		byte[] content = "menu contents".getBytes(StandardCharsets.UTF_8);

		StoredObject stored = storage.store("menus", new ByteArrayInputStream(content));

		assertThat(stored.key()).startsWith("menus/");
		assertThat(stored.size()).isEqualTo(content.length);
		assertThat(stored.sha256())
				.isEqualTo("8bf6c29b0222be96cd008c3acd530714603f04d0790a6dc9f5a2ab22913d39bc");
		try (var input = storage.load(stored.key())) {
			assertThat(input.readAllBytes()).isEqualTo(content);
		}

		storage.delete(stored.key());
		assertThat(temporaryDirectory.resolve(stored.key())).doesNotExist();
	}

	@Test
	void rejectsPathTraversal() {
		FileSystemObjectStorage storage = new FileSystemObjectStorage(temporaryDirectory.toString());

		assertThatThrownBy(() -> storage.load("../secret"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
