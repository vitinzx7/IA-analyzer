package com.resumematch.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CsvCellsTest {

    @Test
    void escreveELeDeVolta() {
        List<String> values = List.of("Java", "AWS / Azure / GCP", "texto, com virgula",
                "aspas \"duplas\"", "=HYPERLINK(\"x\")", "-1");
        String line = String.join(",", values.stream().map(CsvCells::cell).toList());

        assertThat(CsvCells.parse(line)).containsExactlyElementsOf(values);
    }

    @Test
    void neutralizaFormulaAoEscrever() {
        assertThat(CsvCells.cell("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(CsvCells.cell("@cmd")).isEqualTo("'@cmd");
    }

    @Test
    void celulaVaziaOuNulaViraVazio() {
        assertThat(CsvCells.cell(null)).isEmpty();
        assertThat(CsvCells.cell("  ")).isEmpty();
        assertThat(CsvCells.parse("a,,b")).containsExactly("a", "", "b");
    }
}
