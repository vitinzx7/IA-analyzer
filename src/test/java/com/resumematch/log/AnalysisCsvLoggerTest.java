package com.resumematch.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.MatchStatus;
import com.resumematch.matcher.RequirementMatch;

class AnalysisCsvLoggerTest {

    @TempDir
    Path tempDir;

    private AnalysisResult result(RequirementMatch... matches) {
        return new AnalysisResult(50.0, List.of(matches), "resumo");
    }

    @Test
    void gravaCabecalhoEUmaLinhaPorRequisito() throws Exception {
        Path file = tempDir.resolve("analises.csv");
        AnalysisCsvLogger logger = new AnalysisCsvLogger(file, true);

        logger.log(result(
                new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi uma API com Java."),
                new RequirementMatch("Docker", MatchStatus.PARTIAL, "Tecnologias: Docker."),
                new RequirementMatch("AWS", MatchStatus.MISSING, null)), "Java, Docker, AWS");

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).isEqualTo("data,requisito,status,evidencia");
        assertThat(lines.get(1)).contains(",Java,MATCH,");
        assertThat(lines.get(2)).contains(",Docker,PARTIAL,");
        assertThat(lines.get(3)).contains(",AWS,MISSING,");
    }

    @Test
    void naoDuplicaCabecalhoAoRegistrarDuasVezes() throws Exception {
        Path file = tempDir.resolve("analises.csv");
        AnalysisCsvLogger logger = new AnalysisCsvLogger(file, true);
        RequirementMatch match = new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi.");

        logger.log(result(match), "Java");
        logger.log(result(match), "Java");

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(3);
        assertThat(lines.stream().filter("data,requisito,status,evidencia"::equals)).hasSize(1);
    }

    @Test
    void escapaVirgulaEAspas() throws Exception {
        Path file = tempDir.resolve("analises.csv");
        AnalysisCsvLogger logger = new AnalysisCsvLogger(file, true);

        logger.log(result(new RequirementMatch("Java", MatchStatus.MATCH,
                "Desenvolvi \"API\", com Java.")), "Java");

        assertThat(Files.readString(file))
                .contains("\"Desenvolvi \"\"API\"\", com Java.\"");
    }

    @Test
    void naoGravaQuandoDesabilitado() {
        Path file = tempDir.resolve("analises.csv");
        AnalysisCsvLogger logger = new AnalysisCsvLogger(file, false);

        logger.log(result(new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi.")), "Java");

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void criaDiretorioPai() {
        Path file = tempDir.resolve("sub/dir/analises.csv");
        AnalysisCsvLogger logger = new AnalysisCsvLogger(file, true);

        logger.log(result(new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi.")), "Java");

        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void neutralizaFormulaCsvNoInicioDaEvidencia() throws Exception {
        Path file = tempDir.resolve("analises.csv");
        AnalysisCsvLogger logger = new AnalysisCsvLogger(file, true);

        logger.log(result(new RequirementMatch("Java", MatchStatus.PARTIAL, "=HYPERLINK(\"http://x\",\"Java\")")), "Java");

        assertThat(Files.readString(file))
                .contains(",\"'=HYPERLINK(\"\"http://x\"\",\"\"Java\"\")\"");
    }
}
