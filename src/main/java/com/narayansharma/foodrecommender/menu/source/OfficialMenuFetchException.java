package com.narayansharma.foodrecommender.menu.source;

class OfficialMenuFetchException extends RuntimeException {
	OfficialMenuFetchException(String message) {
		super(message);
	}

	OfficialMenuFetchException(String message, Throwable cause) {
		super(message, cause);
	}
}
