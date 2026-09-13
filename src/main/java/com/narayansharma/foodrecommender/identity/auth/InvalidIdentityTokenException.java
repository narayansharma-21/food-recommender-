package com.narayansharma.foodrecommender.identity.auth;

public class InvalidIdentityTokenException extends RuntimeException {
	public InvalidIdentityTokenException(String message) {
		super(message);
	}

	public InvalidIdentityTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
