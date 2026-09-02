package com.resumematch.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.abort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfTextExtractorTest {

    private static final Path CURRICULOS = Path.of("src/test/resources/curriculos");

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extraiTextoDoCurriculoDisponivel() {
        File pdf = anyResume().orElseGet(
                () -> abort("Nenhum PDF em " + CURRICULOS + ", teste ignorado"));

        String text = extractor.extract(pdf);

        assertThat(text).isNotBlank();
    }

    @Test
    void recusaPdfSemTextoSuficiente(@TempDir Path tempDir) throws IOException {
        File quaseVazio = pdfContaining("oi", tempDir.resolve("quase-vazio.pdf"));

        assertThatThrownBy(() -> extractor.extract(quaseVazio))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("caracteres");
    }

    private Optional<File> anyResume() {
        if (!Files.isDirectory(CURRICULOS)) {
            return Optional.empty();
        }
        try (var files = Files.list(CURRICULOS)) {
            return files.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .min(Comparator.naturalOrder())
                    .map(Path::toFile);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private File pdfContaining(String content, Path target) throws IOException {
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
            document.save(target.toFile());
        }
        return target.toFile();
    }
}
