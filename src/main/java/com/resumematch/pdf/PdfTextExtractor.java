package com.resumematch.pdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfTextExtractor {

    static final int MIN_CHARS = 200;

    public String extract(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            return readText(document, file.getName());
        } catch (IOException e) {
            throw new PdfExtractionException("Nao foi possivel abrir o PDF: " + file.getName(), e);
        }
    }

    public String extract(InputStream input, String origin) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(input))) {
            return readText(document, origin);
        } catch (IOException e) {
            throw new PdfExtractionException("Nao foi possivel abrir o PDF: " + origin, e);
        }
    }

    private String readText(PDDocument document, String origin) throws IOException {
        String text = new PDFTextStripper().getText(document).strip();
        if (text.length() < MIN_CHARS) {
            throw new PdfExtractionException(
                    "Texto extraido de " + origin + " tem apenas " + text.length()
                            + " caracteres, minimo esperado e " + MIN_CHARS
                            + ". O arquivo pode ser digitalizado ou conter apenas imagens."
                            + " A analise foi interrompida para nao gerar um score falso.");
        }
        return text;
    }
}
