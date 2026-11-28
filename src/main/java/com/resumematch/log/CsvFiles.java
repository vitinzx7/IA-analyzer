package com.resumematch.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Acrescenta linhas a um CSV local, escrevendo o cabecalho quando o arquivo e
 * novo ou esta vazio.
 */
final class CsvFiles {

    private CsvFiles() {
    }

    /**
     * @param rows linhas ja montadas, cada uma terminada em quebra de linha
     */
    static void append(Path path, String header, String rows) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder content = new StringBuilder();
        if (!Files.exists(path) || Files.size(path) == 0) {
            content.append(header).append('\n');
        } else if (!endsWithNewline(path)) {
            // Planilha que salvou o arquivo sem a ultima quebra de linha.
            content.append('\n');
        }
        content.append(rows);
        Files.writeString(path, content.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static boolean endsWithNewline(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.length() == 0) {
                return true;
            }
            file.seek(file.length() - 1);
            return file.read() == '\n';
        }
    }
}
