package com.narayansharma.foodrecommender.menu.source;

import com.narayansharma.foodrecommender.menu.catalog.MenuCatalogService;
import com.narayansharma.foodrecommender.menu.catalog.MenuDefinition;
import java.net.URI;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OfficialMenuSubmissionService {
	private final MenuCatalogService menuCatalogService;
	private final OfficialMenuSourceService sourceService;
	private final MenuSourceFetchJobQueue fetchJobQueue;

	OfficialMenuSubmissionService(
			MenuCatalogService menuCatalogService,
			OfficialMenuSourceService sourceService,
			MenuSourceFetchJobQueue fetchJobQueue) {
		this.menuCatalogService = menuCatalogService;
		this.sourceService = sourceService;
		this.fetchJobQueue = fetchJobQueue;
	}

	@Transactional
	OfficialMenuSubmissionResponse submit(
			UUID locationId,
			String menuKey,
			String displayName,
			MenuSourceType sourceType,
			URI url) {
		MenuDefinition menu = menuCatalogService.findOrCreate(locationId, menuKey, displayName);
		UUID sourceId = sourceService.register(menu.menuId(), sourceType, url);
		fetchJobQueue.enqueue(sourceId);
		return new OfficialMenuSubmissionResponse(menu.menuId(), sourceId, "QUEUED");
	}
}
