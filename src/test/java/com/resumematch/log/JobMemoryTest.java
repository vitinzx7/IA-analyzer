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
import com.resumematch.matcher.PreviousAnalysis;
import com.resumematch.matcher.RequirementMatch;
import com.resumematch.matcher.RequirementPriority;

class JobMemoryTest {

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

        new JobMemory(file, true).log(result(), JOB);

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).isEqualTo(JobMemory.HEADER);
        List<String> cells = row(file, 1);
        assertThat(cells).hasSize(JobMemory.HEADER.split(",").length);
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

        new JobMemory(file, true).log(result(), JOB);

        assertThat(Files.readString(file))
                .doesNotContain("Desenvolvi uma API")
                .doesNotContain("Tecnologias:");
    }

    @Test
    void mesmaVagaDuasVezesGanhaDuasLinhasComAMesmaChave() throws Exception {
        Path file = tempDir.resolve("vagas.csv");
        JobMemory logger = new JobMemory(file, true);

        logger.log(result(), JOB);
        logger.log(result(), JOB);

        assertThat(Files.readAllLines(file)).hasSize(3);
        assertThat(row(file, 1).get(1)).isEqualTo(row(file, 2).get(1));
    }

    @Test
    void semRequisitoReconhecidoGravaCoberturaVazia() throws Exception {
        Path file = tempDir.resolve("vagas.csv");

        new JobMemory(file, true).log(
                new AnalysisResult(null, List.of(), "resumo", List.of(), List.of("Elixir")), "Vaga de Elixir.");

        List<String> cells = row(file, 1);
        assertThat(cells.get(1)).startsWith("texto:");
        assertThat(cells.get(6)).isEmpty();
        assertThat(cells.get(13)).isEqualTo("Elixir");
    }

    @Test
    void neutralizaFormulaVindaDaVaga() throws Exception {
        Path file = tempDir.resolve("vagas.csv");

        new JobMemory(file, true).log(result(),
                "=HYPERLINK(\"http://x\")\nEmpresa\nhttps://www.linkedin.com/jobs/view/1/");

        assertThat(row(file, 1).get(2)).isEqualTo("=HYPERLINK(\"http://x\")");
        assertThat(Files.readString(file)).contains("'=HYPERLINK");
    }

    @Test
    void naoGravaQuandoDesabilitado() {
        Path file = tempDir.resolve("vagas.csv");

        new JobMemory(file, false).log(result(), JOB);

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void falhaDeEscritaNaoDerrubaAAnalise() throws Exception {
        Path directory = Files.createDirectory(tempDir.resolve("vagas.csv"));

        assertThatCode(() -> new JobMemory(directory, true).log(result(), JOB))
                .doesNotThrowAnyException();
    }

    @Test
    void vagaNovaNaoTemAnaliseAnterior() {
        JobMemory memory = new JobMemory(tempDir.resolve("vagas.csv"), true);

        assertThat(memory.previous(JOB)).isNull();
    }

    @Test
    void contaAsAnalisesAnterioresDaMesmaVagaEDevolveAUltima() {
        JobMemory memory = new JobMemory(tempDir.resolve("vagas.csv"), true);
        memory.log(new AnalysisResult(40.0, List.of(), "r"), JOB);
        memory.log(result(), JOB);
        memory.log(result(), "Outra vaga: Python.");

        PreviousAnalysis previous = memory.previous(JOB);

        assertThat(previous.count()).isEqualTo(2);
        assertThat(previous.lastCoverage()).isEqualTo(55.0);
        assertThat(previous.lastAt()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void memoriaDesligadaNaoConsultaNemConta() {
        JobMemory memory = new JobMemory(tempDir.resolve("vagas.csv"), false);

        assertThat(memory.previous(JOB)).isNull();
        assertThat(memory.patterns().enabled()).isFalse();
    }

    @Test
    void padroesContamCadaVagaUmaVezPelaAnaliseMaisRecente() {
        JobMemory memory = new JobMemory(tempDir.resolve("vagas.csv"), true);
        memory.log(new AnalysisResult(0.0, List.of(
                new RequirementMatch("Cobol", MatchStatus.MISSING, null)), "r"), JOB);
        memory.log(result(), JOB);
        memory.log(new AnalysisResult(50.0, List.of(
                new RequirementMatch("Java", MatchStatus.MISSING, null),
                new RequirementMatch("AWS", MatchStatus.MATCH, "Usei AWS."),
                new RequirementMatch("Linux", MatchStatus.PARTIAL, "Linux.", RequirementPriority.PREFERRED)),
                "r", List.of(), List.of("Kotlin")), "Outra vaga.");

        JobPatterns patterns = memory.patterns();

        assertThat(patterns.enabled()).isTrue();
        assertThat(patterns.jobs()).isEqualTo(2);
        assertThat(patterns.analyses()).isEqualTo(3);
        assertThat(patterns.requirements()).extracting(JobPatterns.Requirement::name)
                .startsWith("AWS", "Java")
                .doesNotContain("Cobol");
        assertThat(patterns.requirements()).contains(
                new JobPatterns.Requirement("AWS", 2, 1, 1),
                new JobPatterns.Requirement("Java", 2, 2, 0),
                new JobPatterns.Requirement("Linux", 1, 0, 1));
        assertThat(patterns.unknownTerms()).containsExactly(
                new JobPatterns.Term("Kotlin", 2), new JobPatterns.Term("Quarkus", 1));
    }

    @Test
    void padroesSemArquivoDevolvemZero() {
        JobPatterns patterns = new JobMemory(tempDir.resolve("vagas.csv"), true).patterns();

        assertThat(patterns.jobs()).isZero();
        assertThat(patterns.requirements()).isEmpty();
    }

    @Test
    void linhaEstragadaEPuladaNaLeitura() throws Exception {
        Path file = tempDir.resolve("vagas.csv");
        JobMemory memory = new JobMemory(file, true);
        memory.log(result(), JOB);
        Files.writeString(file, "linha,quebrada\n", java.nio.file.StandardOpenOption.APPEND);

        assertThat(memory.patterns().jobs()).isEqualTo(1);
        assertThat(memory.previous(JOB).count()).isEqualTo(1);
    }
}
