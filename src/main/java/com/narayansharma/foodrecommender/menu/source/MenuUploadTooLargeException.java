package com.narayansharma.foodrecommender.menu.source;

public class MenuUploadTooLargeException extends RuntimeException {
	public MenuUploadTooLargeException() {
		super("Uploaded menu image exceeds the size limit");
	}
}
