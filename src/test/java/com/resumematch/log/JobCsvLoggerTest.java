package com.resumematch.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.MatchStatus;
import com.resumematch.matcher.RequirementMatch;
import com.resumematch.matcher.RequirementPriority;

class JobCsvLoggerTest {

    private static final String JOB = """
            Desenvolvedor Java Junior
            Empresa Exemplo
            Brasilia, DF · Remoto
            https://www.linkedin.com/jobs/view/4123456789/

            Sobre a vaga
            Requisitos: Java, SQL, Docker. Diferencial: AWS, Kubernetes.
            """;

    @TempDir
    Path tempDir;

    private static AnalysisResult result() {
        return new AnalysisResult(55.0, List.of(
                new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi uma API em Java no projeto X."),
                new RequirementMatch("SQL", MatchStatus.PARTIAL, "Tecnologias: SQL."),
                new RequirementMatch("Docker", MatchStatus.MISSING, null),
                new RequirementMatch("AWS", MatchStatus.MISSING, null, RequirementPriority.PREFERRED),
                new RequirementMatch("Kubernetes", MatchStatus.MISSING, null, RequirementPriority.PREFERRED)),
                "resumo", List.of(), List.of("Kotlin", "Quarkus"));
    }

    private static List<String> row(Path file, int index) throws Exception {
        return CsvCells.parse(Files.readAllLines(file).get(index));
    }

    @Test
    void gravaUmaLinhaPorAnaliseComAVagaEOsRequisitosSeparados() throws Exception {
        Path file = tempDir.resolve("vagas.csv");

        new JobCsvLogger(file, true).log(result(), JOB);

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).isEqualTo(JobCsvLogger.HEADER);
        List<String> cells = row(file, 1);
        assertThat(cells).hasSize(JobCsvLogger.HEADER.split(",").length);
        assertThat(cells.get(0)).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        assertThat(cells.subList(1, 14)).containsExactly(
                "linkedin:4123456789",
                "Desenvolvedor Java Junior",
                "Empresa Exemplo",
                "Brasilia, DF · Remoto",
                "https://www.linkedin.com/jobs/view/4123456789/",
                "55.0",
                "Java", "SQL", "Docker",
                "", "", "AWS; Kubernetes",
                "Kotlin; Quarkus");
    }

    @Test
    void naoGravaTrechoDoCurriculo() throws Exception {
        Path file = tempDir.resolve("vagas.csv");

        new JobCsvLogger(file, true).log(result(), JOB);

        assertThat(Files.readString(file))
                .doesNotContain("Desenvolvi uma API")
                .doesNotContain("Tecnologias:");
    }

    @Test
    void mesmaVagaDuasVezesGanhaDuasLinhasComAMesmaChave() throws Exception {
        Path file = tempDir.resolve("vagas.csv");
        JobCsvLogger logger = new JobCsvLogger(file, true);

        logger.log(result(), JOB);
        logger.log(result(), JOB);

        assertThat(Files.readAllLines(file)).hasSize(3);
        assertThat(row(file, 1).get(1)).isEqualTo(row(file, 2).get(1));
    }

    @Test
    void semRequisitoReconhecidoGravaCoberturaVazia() throws Exception {
        Path file = tempDir.resolve("vagas.csv");

        new JobCsvLogger(file, true).log(
                new AnalysisResult(null, List.of(), "resumo", List.of(), List.of("Elixir")), "Vaga de Elixir.");

        List<String> cells = row(file, 1);
        assertThat(cells.get(1)).startsWith("texto:");
        assertThat(cells.get(6)).isEmpty();
        assertThat(cells.get(13)).isEqualTo("Elixir");
    }

    @Test
    void neutralizaFormulaVindaDaVaga() throws Exception {
        Path file = tempDir.resolve("vagas.csv");

        new JobCsvLogger(file, true).log(result(),
                "=HYPERLINK(\"http://x\")\nEmpresa\nhttps://www.linkedin.com/jobs/view/1/");

        assertThat(row(file, 1).get(2)).isEqualTo("=HYPERLINK(\"http://x\")");
        assertThat(Files.readString(file)).contains("'=HYPERLINK");
    }

    @Test
    void naoGravaQuandoDesabilitado() {
        Path file = tempDir.resolve("vagas.csv");

        new JobCsvLogger(file, false).log(result(), JOB);

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void falhaDeEscritaNaoDerrubaAAnalise() throws Exception {
        Path directory = Files.createDirectory(tempDir.resolve("vagas.csv"));

        assertThatCode(() -> new JobCsvLogger(directory, true).log(result(), JOB))
                .doesNotThrowAnyException();
    }
}
