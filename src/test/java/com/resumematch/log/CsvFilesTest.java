package com.resumematch.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void escreveCabecalhoSoNoArquivoNovo() throws Exception {
        Path file = tempDir.resolve("x.csv");

        CsvFiles.append(file, "a,b", "1,2\n");
        CsvFiles.append(file, "a,b", "3,4\n");

        assertThat(Files.readAllLines(file)).containsExactly("a,b", "1,2", "3,4");
    }

    @Test
    void completaLinhaQueAPlanilhaDeixouSemQuebra() throws Exception {
        Path file = tempDir.resolve("x.csv");
        Files.writeString(file, "a,b\n1,2");

        CsvFiles.append(file, "a,b", "3,4\n");

        assertThat(Files.readAllLines(file)).containsExactly("a,b", "1,2", "3,4");
    }
}
