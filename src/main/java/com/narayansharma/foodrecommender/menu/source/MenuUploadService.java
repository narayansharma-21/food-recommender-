package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.menu.catalog.MenuDefinition;
import com.narayansharma.foodrecommender.menu.catalog.MenuCatalogService;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuUploadService {
	private final MenuCatalogService menuCatalogService;
	private final UserMenuImageSourceService imageSourceService;

	public MenuUploadService(
			MenuCatalogService menuCatalogService,
			UserMenuImageSourceService imageSourceService) {
		this.menuCatalogService = menuCatalogService;
		this.imageSourceService = imageSourceService;
	}

	@Transactional
	public MenuUploadResponse upload(
			UUID userId,
			UUID locationId,
			String menuKey,
			String displayName,
			String mediaType,
			InputStream content) {
		if (userId == null) {
			throw new IllegalArgumentException("Signed-in user is required");
		}
		MenuDefinition menu = menuCatalogService.findOrCreate(locationId, menuKey, displayName);
		UploadedMenuImageSource uploaded = imageSourceService.upload(
				menu.menuId(), userId.toString(), mediaType, content);
		return new MenuUploadResponse(
				menu.menuId(),
				uploaded.sourceId(),
				uploaded.versionId(),
				"PROCESSING",
				uploaded.mediaType(),
				uploaded.sizeBytes(),
				uploaded.sha256());
	}
}
