package com.narayansharma.foodrecommender.platform.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorage {
	StoredObject store(String namespace, InputStream content) throws IOException;

	InputStream load(String key) throws IOException;

	void delete(String key) throws IOException;
}
