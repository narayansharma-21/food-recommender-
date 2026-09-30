package com.narayansharma.foodrecommender.menu.source;

import java.net.URI;

record FetchedOfficialMenu(URI finalUrl, String mediaType, byte[] content) {
	FetchedOfficialMenu {
		content = content.clone();
	}

	@Override
	public byte[] content() {
		return content.clone();
	}
}
