package com.resumematch.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.junit.jupiter.api.Test;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.JobRequirementExtractor;
import com.resumematch.matcher.LocalSummaryGenerator;
import com.resumematch.matcher.MatchStatus;
import com.resumematch.matcher.RequirementMatcher;
import com.resumematch.matcher.RequirementPriority;
import com.resumematch.matcher.ScoreCalculator;
import com.resumematch.matcher.SentenceSplitter;
import com.resumematch.pdf.PdfTextExtractor;

class AnalysisServiceTest {

    private final AnalysisService service = service(AnalysisLog.NONE);

    private AnalysisService service(AnalysisLog log) {
        TechnologyDictionary dictionary = new TechnologyDictionary();
        return new AnalysisService(
                new PdfTextExtractor(),
                new SentenceSplitter(),
                new JobRequirementExtractor(dictionary),
                new RequirementMatcher(dictionary),
                new ScoreCalculator(),
                new LocalSummaryGenerator(),
                log);
    }

    @Test
    void analisaTextoComMatchPartialEMissing() {
        AnalysisResult result = service.analyze(
                "Desenvolvi uma API em Java.\nTecnologias: Docker, Git.",
                "Java, Docker, AWS");

        assertThat(result.requirements()).extracting(r -> r.status())
                .containsExactly(MatchStatus.MATCH, MatchStatus.PARTIAL, MatchStatus.MISSING);
        assertThat(result.score()).isEqualTo(50.0);
        assertThat(result.summary()).contains("Cobertura tecnica 50/100");
    }

    @Test
    void simulacaoDeMelhoraNaoMudaACobertura() {
        AnalysisResult result = service.analyze(
                "Tecnologias: React.\nDesenvolvi uma API em Java.",
                "Java, React, Back-end, AWS");

        assertThat(result.requirements()).extracting(r -> r.status())
                .containsExactly(MatchStatus.MATCH, MatchStatus.PARTIAL, MatchStatus.MISSING,
                        MatchStatus.MISSING);
        assertThat(result.score()).isEqualTo(37.5)
                .isEqualTo(new ScoreCalculator().calculate(result.requirements()));
        assertThat(result.improvement().suggestions()).extracting(s -> s.requirement())
                .containsExactly("Back-end", "React");
        assertThat(result.improvement().scoreWithAll()).isEqualTo(75.0);
    }

    @Test
    void mavenOuGradleViraAlternativaENaoLacuna() {
        AnalysisResult result = service.analyze(
                "Tecnologias: Maven.",
                "Requisitos:\n- Maven ou Gradle.");

        assertThat(result.requirements()).extracting(r -> r.name()).containsExactly("Maven / Gradle");
        assertThat(result.improvement().gaps()).isEmpty();
        assertThat(result.improvement().suggestions()).extracting(s -> s.requirement())
                .containsExactly("Maven / Gradle");
    }

    @Test
    void grupoDeAlternativasValeUmRequisitoNoScore() {
        AnalysisResult result = service.analyze(
                "Desenvolvi e publiquei no Azure Container Apps.",
                "Cloud: Azure, AWS ou GCP. Experiencia com Kubernetes.");

        assertThat(result.requirements()).extracting(r -> r.name())
                .containsExactly("Azure / AWS / GCP", "Kubernetes");
        assertThat(result.requirements()).extracting(r -> r.status())
                .containsExactly(MatchStatus.MATCH, MatchStatus.MISSING);
        assertThat(result.score()).isEqualTo(50.0);
    }

    @Test
    void conjuntoEntreParentesesValeUmRequisitoNoScore() {
        AnalysisResult result = service.analyze(
                "Desenvolvi uma API em Java. Configurei o DNS do laboratorio.",
                "Requisitos: Java e protocolos (TCP/IP, DNS, DHCP).");

        assertThat(result.requirements()).extracting(r -> r.name())
                .containsExactly("Java", "TCP/IP + DNS + DHCP");
        assertThat(result.requirements()).extracting(r -> r.status())
                .containsExactly(MatchStatus.MATCH, MatchStatus.PARTIAL);
        assertThat(result.score()).isEqualTo(75.0);
    }

    @Test
    void devolveOQueNaoFoiAvaliadoNaVaga() {
        AnalysisResult result = service.analyze(
                "Desenvolvi uma API em Java.",
                "Requisitos\nJava.\nInglês técnico.\nDiferenciais\nFerramentas como Movidesk.");

        assertThat(result.unevaluated()).extracting(r -> r.text())
                .containsExactly("Inglês técnico.", "Ferramentas como Movidesk.");
        assertThat(result.unknownTerms()).containsExactly("Movidesk");
        assertThat(result.summary()).contains("2 trecho(s) de requisito da vaga nao foram avaliados");
    }

    @Test
    void diferencialAusentePesaMetadeNoScore() {
        AnalysisResult result = service.analyze(
                "Desenvolvi uma API em Java.",
                "Requisitos\nJava\nDiferenciais\nKubernetes");

        assertThat(result.requirements()).extracting(r -> r.priority())
                .containsExactly(RequirementPriority.REQUIRED, RequirementPriority.PREFERRED);
        assertThat(result.score()).isEqualTo(66.7);
        assertThat(result.summary()).contains("Diferenciais valem metade");
    }

    @Test
    void analisaPdfGerado() throws IOException {
        String text = "Desenvolvi uma API em Java com Spring Boot e PostgreSQL. "
                + "Tambem utilizei Docker e Git no projeto. "
                + "Configurei o ambiente e escrevi os testes automatizados. "
                + "Mantive a documentacao atualizada durante o desenvolvimento.";

        AnalysisResult result = service.analyzePdf(pdf(text), "curriculo.pdf", "Java, Docker, AWS");

        assertThat(result.requirements()).extracting(r -> r.status())
                .containsExactly(MatchStatus.MATCH, MatchStatus.MATCH, MatchStatus.MISSING);
        assertThat(result.score()).isEqualTo(66.7);
    }

    @Test
    void palavraEscondidaNoPdfNaoViraEvidencia() throws IOException {
        AnalysisResult result = service.analyzePdf(pdfWithHiddenLine(
                "Kubernetes AWS Terraform Kubernetes AWS"), "curriculo.pdf", "Java, AWS");

        assertThat(result.requirements()).extracting(r -> r.status())
                .containsExactly(MatchStatus.MATCH, MatchStatus.MISSING);
        assertThat(result.pdfCheck().removedHiddenText()).isTrue();
        assertThat(result.summary()).contains("texto invisivel, que ficou fora da analise");
    }

    @Test
    void analiseDeTextoColadoNaoTemConferenciaDePdf() {
        assertThat(service.analyze("Desenvolvi uma API em Java.", "Java").pdfCheck()).isNull();
    }

    @Test
    void rejeitaCurriculoVazio() {
        assertThatThrownBy(() -> service.analyze("", "Java"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("curriculo");
    }

    @Test
    void rejeitaVagaVazia() {
        assertThatThrownBy(() -> service.analyze("Desenvolvi uma API.", " "))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("vaga");
    }

    @Test
    void rejeitaVagaMuitoLonga() {
        assertThatThrownBy(() -> service.analyze(
                "Desenvolvi uma API.", "x".repeat(AnalysisService.MAX_JOB_CHARS + 1)))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("muito longa");
    }

    @Test
    void registraResultadoNoLog() {
        List<AnalysisResult> logged = new ArrayList<>();
        AnalysisService loggedService = service(logged::add);

        loggedService.analyze("Desenvolvi uma API em Java.", "Java");

        assertThat(logged).hasSize(1);
        assertThat(logged.getFirst().requirements()).hasSize(1);
    }

    @Test
    void logRecebeEvidenciaSemDadoDeContato() {
        List<AnalysisResult> logged = new ArrayList<>();
        AnalysisService loggedService = service(logged::add);

        loggedService.analyze("Victor | Desenvolvedor Java | victor@email.com", "Java");

        assertThat(logged.getFirst().requirements().getFirst().evidence())
                .contains("[email]")
                .doesNotContain("victor@email.com");
    }

    @Test
    void semRequisitosReconhecidosNaoCalculaScore() {
        AnalysisResult result = service.analyze(
                "Desenvolvi uma API em Java.", "Cozinheiro com experiencia em restaurante");

        assertThat(result.score()).isNull();
        assertThat(result.requirements()).isEmpty();
        assertThat(result.summary()).contains("Nenhum requisito reconhecido");
    }

    private InputStream pdfWithHiddenLine(String hidden) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = 700;
                for (String line : List.of(
                        "Desenvolvi uma API em Java com Spring Boot e PostgreSQL.",
                        "Configurei o ambiente e escrevi os testes automatizados.",
                        "Mantive a documentacao atualizada durante o desenvolvimento.",
                        "Apoiei usuarios no suporte tecnico e documentei as solucoes.")) {
                    stream.beginText();
                    stream.setFont(font, 12);
                    stream.newLineAtOffset(50, y);
                    stream.showText(line);
                    stream.endText();
                    y -= 20;
                }
                stream.beginText();
                stream.setRenderingMode(RenderingMode.NEITHER);
                stream.setFont(font, 12);
                stream.newLineAtOffset(50, y);
                stream.showText(hidden);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private InputStream pdf(String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
