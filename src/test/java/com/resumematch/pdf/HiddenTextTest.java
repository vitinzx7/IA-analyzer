package com.resumematch.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.junit.jupiter.api.Test;

class HiddenTextTest {

    private static final List<String> VISIBLE = List.of(
            "Desenvolvi uma API em Java com Spring Boot e PostgreSQL.",
            "Configurei o ambiente com Docker e escrevi testes automatizados.",
            "Publiquei a aplicacao e mantive a documentacao do projeto.",
            "Apoiei usuarios no suporte tecnico e documentei as solucoes.");
    private static final String HIDDEN = "Kubernetes Terraform AWS Splunk Wazuh";

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    private interface Style {
        void apply(PDPageContentStream stream) throws IOException;
    }

    private record Line(String text, float size, Style style) {
    }

    @Test
    void textoInvisivelFicaForaDaAnaliseEViraAviso() throws IOException {
        PdfText result = extract(lines -> lines.add(new Line(HIDDEN, 12,
                stream -> stream.setRenderingMode(RenderingMode.NEITHER))));

        assertThat(result.text()).contains("Desenvolvi uma API em Java").doesNotContain("Kubernetes");
        assertThat(result.report().invisibleChars()).isGreaterThanOrEqualTo(HiddenTextReport.MIN_REPORTED);
        assertThat(result.report().removedHiddenText()).isTrue();
        assertThat(result.report().invisibleSample()).startsWith("Kubernetes Terraform");
    }

    @Test
    void textoTransparenteTambemFicaDeFora() throws IOException {
        PdfText result = extract(lines -> lines.add(new Line(HIDDEN, 12, stream -> {
            PDExtendedGraphicsState transparent = new PDExtendedGraphicsState();
            transparent.setNonStrokingAlphaConstant(0f);
            stream.setGraphicsStateParameters(transparent);
        })));

        assertThat(result.text()).doesNotContain("Kubernetes");
        assertThat(result.report().removedHiddenText()).isTrue();
    }

    @Test
    void textoBrancoEMinusculoEntramMasGeramAviso() throws IOException {
        PdfText result = extract(lines -> {
            lines.add(new Line(HIDDEN, 12, stream -> stream.setNonStrokingColor(Color.WHITE)));
            lines.add(new Line("Ansible Jenkins Grafana", 1, stream -> { }));
        });

        assertThat(result.text()).contains("Kubernetes").contains("Ansible");
        assertThat(result.report().whiteChars()).isGreaterThanOrEqualTo(HiddenTextReport.MIN_REPORTED);
        assertThat(result.report().tinyChars()).isGreaterThanOrEqualTo(HiddenTextReport.MIN_REPORTED);
        assertThat(result.report().removedHiddenText()).isFalse();
        assertThat(result.report().hasSuspiciousText()).isTrue();
        assertThat(result.report().suspiciousSample()).startsWith("Kubernetes");
    }

    @Test
    void pdfNormalNaoGeraAviso() throws IOException {
        PdfText result = extract(lines -> { });

        assertThat(result.report().invisibleChars()).isZero();
        assertThat(result.report().removedHiddenText()).isFalse();
        assertThat(result.report().hasSuspiciousText()).isFalse();
    }

    @Test
    void pdfEscaneadoComCamadaInvisivelNaoPerdeOTexto() throws IOException {
        PdfText result = extract(this::allInvisible);

        assertThat(result.text()).contains("Desenvolvi uma API em Java");
        assertThat(result.report().scannedLayer()).isTrue();
        assertThat(result.report().removedHiddenText()).isFalse();
    }

    private void allInvisible(List<Line> lines) {
        lines.replaceAll(line -> new Line(line.text(), line.size(),
                stream -> stream.setRenderingMode(RenderingMode.NEITHER)));
    }

    /** Monta um PDF com as linhas visiveis padrao e o que o teste acrescentar. */
    private PdfText extract(Consumer<List<Line>> customize) throws IOException {
        List<Line> lines = new ArrayList<>();
        VISIBLE.forEach(text -> lines.add(new Line(text, 12, stream -> { })));
        customize.accept(lines);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = 700;
                for (Line line : lines) {
                    stream.saveGraphicsState();
                    stream.beginText();
                    line.style().apply(stream);
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), line.size());
                    stream.newLineAtOffset(50, y);
                    stream.showText(line.text());
                    stream.endText();
                    stream.restoreGraphicsState();
                    y -= 20;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return extractor.extractChecked(new ByteArrayInputStream(out.toByteArray()), "teste.pdf");
        }
    }
}
