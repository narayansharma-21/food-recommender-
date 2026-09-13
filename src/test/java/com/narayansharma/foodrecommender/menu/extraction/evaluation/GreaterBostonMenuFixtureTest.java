package com.narayansharma.foodrecommender.menu.extraction.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class GreaterBostonMenuFixtureTest {
	private static final Set<String> APPROVED_SOURCE_HOSTS = Set.of(
			"www.legalseafoods.com",
			"www.flourbakery.com",
			"sisterscaribbeanrestaurant.com");

	@Test
	void fixturesAreSmallTraceableAndStructurallyComplete() throws IOException {
		List<GreaterBostonMenuFixture> fixtures = loadFixtures();

		assertThat(fixtures).hasSize(3);
		Set<String> ids = new HashSet<>();
		for (GreaterBostonMenuFixture fixture : fixtures) {
			assertThat(ids.add(fixture.id())).isTrue();
			assertThat(fixture.restaurant()).isNotBlank();
			assertThat(fixture.municipality()).isIn("Boston", "Somerville");
			URI source = URI.create(fixture.sourceUrl());
			assertThat(source.getScheme()).isEqualTo("https");
			assertThat(source.getHost()).isIn(APPROVED_SOURCE_HOSTS);
			assertThat(LocalDate.parse(fixture.capturedDate())).isNotNull();
			assertThat(fixture.ocrText().length()).isLessThan(1_000);
			assertThat(fixture.expectedSections()).isNotEmpty();
			assertThat(fixture.expectedSections())
					.allSatisfy(section -> assertThat(section.items()).isNotEmpty());
		}
	}

	static List<GreaterBostonMenuFixture> loadFixtures() throws IOException {
		try (var input = GreaterBostonMenuFixtureTest.class.getResourceAsStream(
				"/menu-extraction/greater-boston/fixtures.json")) {
			assertThat(input).isNotNull();
			return new ObjectMapper().readValue(input, new TypeReference<>() {});
		}
	}
}
