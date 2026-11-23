package com.resumematch.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.resumematch.analysis.AnalysisLog;
import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.RequirementMatch;

/**
 * Grava uma linha por decisao do matcher num CSV local.
 *
 * O arquivo fica fora do Git e pode conter trechos do curriculo; por isso o
 * caminho padrao e um arquivo local e o recurso pode ser desligado.
 */
public final class AnalysisCsvLogger implements AnalysisLog {

    private static final Logger LOG = LoggerFactory.getLogger(AnalysisCsvLogger.class);
    private static final String HEADER = "data,requisito,status,evidencia";

    private final Path path;
    private final boolean enabled;

    public AnalysisCsvLogger(Path path, boolean enabled) {
        this.path = path;
        this.enabled = enabled;
    }

    @Override
    public synchronized void log(AnalysisResult result) {
        if (!enabled || result == null) {
            return;
        }
        try {
            createParentDirectory();
            StringBuilder content = new StringBuilder();
            if (!Files.exists(path) || Files.size(path) == 0) {
                content.append(HEADER).append('\n');
            } else if (!endsWithNewline()) {
                content.append('\n');
            }
            String date = LocalDate.now().toString();
            for (RequirementMatch requirement : result.requirements()) {
                content.append(csv(date)).append(',')
                        .append(csv(requirement.name())).append(',')
                        .append(csv(requirement.status().name())).append(',')
                        .append(csv(requirement.evidence())).append('\n');
            }
            Files.writeString(path, content.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            LOG.warn("Nao foi possivel gravar o CSV de analises em {}", path, e);
        }
    }

    private void createParentDirectory() throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private boolean endsWithNewline() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.length() == 0) {
                return true;
            }
            file.seek(file.length() - 1);
            return file.read() == '\n';
        }
    }

    private String csv(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String clean = value.replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .strip();
        if (clean.contains(",") || clean.contains("\"")) {
            return "\"" + clean.replace("\"", "\"\"") + "\"";
        }
        return clean;
    }
}
