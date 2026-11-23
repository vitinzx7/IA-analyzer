package com.resumematch.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.web.multipart.MultipartFile;

import com.resumematch.analysis.AnalysisException;

/**
 * Confere o conteudo do upload, nao apenas a extensao ou o content-type.
 */
final class PdfUploadValidator {

    static final long MAX_PDF_BYTES = 10L * 1024 * 1024;

    private PdfUploadValidator() {
    }

    static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AnalysisException("Envie um arquivo PDF.");
        }
        if (file.getSize() > MAX_PDF_BYTES) {
            throw new AnalysisException("O PDF enviado e muito grande.");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(1024);
            String prefix = new String(header, StandardCharsets.ISO_8859_1);
            if (!prefix.contains("%PDF-")) {
                throw new AnalysisException("O arquivo enviado nao e um PDF valido.");
            }
        } catch (IOException e) {
            throw new AnalysisException("Nao foi possivel ler o arquivo enviado.", e);
        }
    }
}
