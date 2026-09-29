package com.narayansharma.foodrecommender.menu.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.narayansharma.foodrecommender.menu.catalog.MenuCatalogService;
import com.narayansharma.foodrecommender.menu.catalog.MenuDefinition;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuUploadServiceTest {
	@Test
	void createsTheMenuAndHidesTheInternalStorageKey() {
		UUID userId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		MenuCatalogService catalog = mock(MenuCatalogService.class);
		UserMenuImageSourceService images = mock(UserMenuImageSourceService.class);
		when(catalog.findOrCreate(locationId, "main", "Main Menu"))
				.thenReturn(new MenuDefinition(menuId, locationId, "main", "Main Menu", true));
		when(images.upload(
				org.mockito.ArgumentMatchers.eq(menuId),
				org.mockito.ArgumentMatchers.eq(userId.toString()),
				org.mockito.ArgumentMatchers.eq("image/png"),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(new UploadedMenuImageSource(
						sourceId, versionId, "menu-images/private-key", "image/png", 9, "a".repeat(64)));

		MenuUploadResponse response = new MenuUploadService(catalog, images).upload(
				userId,
				locationId,
				"main",
				"Main Menu",
				"image/png",
				new ByteArrayInputStream(new byte[] {1}));

		assertThat(response.menuId()).isEqualTo(menuId);
		assertThat(response.menuVersionId()).isEqualTo(versionId);
		assertThat(response.extractionStatus()).isEqualTo("PROCESSING");
		assertThat(response.toString()).doesNotContain("private-key");
	}
}
