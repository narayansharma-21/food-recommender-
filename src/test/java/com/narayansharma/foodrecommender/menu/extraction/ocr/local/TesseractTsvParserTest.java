package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TesseractTsvParserTest {
	private final TesseractTsvParser parser = new TesseractTsvParser();

	@Test
	void rebuildsLinesAndAveragesValidWordConfidence() {
		String tsv = """
				level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext
				5\t1\t1\t1\t1\t1\t0\t0\t10\t10\t90.0\tMargherita
				5\t1\t1\t1\t1\t2\t11\t0\t10\t10\t80.0\tPizza
				5\t1\t1\t1\t2\t1\t0\t12\t10\t10\t-1\t14
				""";

		TesseractOutput result = parser.parse(tsv);

		assertThat(result.text()).isEqualTo("Margherita Pizza\n14");
		assertThat(result.confidence()).isEqualTo(0.85);
	}
}
