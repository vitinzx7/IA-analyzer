package com.resumematch.feedback;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.resumematch.log.CsvCells;
import com.resumematch.matcher.MatchStatus;

/**
 * Guarda as marcacoes num CSV local, uma linha por marcacao.
 *
 * O arquivo fica fora do Git (*.csv) e nao tem trecho do curriculo. Desligado,
 * nada e gravado e a leitura devolve lista vazia.
 */
public final class FeedbackStore {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackStore.class);
    static final String HEADER = "data,analise,requisito,status_programa,status_correto";

    private final Path path;
    private final boolean enabled;

    public FeedbackStore(Path path, boolean enabled) {
        this.path = path;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public synchronized void record(Feedback feedback) {
        if (!enabled) {
            return;
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder line = new StringBuilder();
            if (!Files.exists(path) || Files.size(path) == 0) {
                line.append(HEADER).append('\n');
            }
            line.append(LocalDate.now()).append(',')
                    .append(CsvCells.cell(feedback.analysisId())).append(',')
                    .append(CsvCells.cell(feedback.requirement())).append(',')
                    .append(feedback.programStatus().name()).append(',')
                    .append(feedback.correctStatus().name()).append('\n');
            Files.writeString(path, line.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar a marcacao em " + path, e);
        }
    }

    /** Linhas que nao puderem ser lidas sao puladas, com aviso no log. */
    public synchronized List<Feedback> all() {
        if (!enabled || !Files.exists(path)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + path, e);
        }
        List<Feedback> feedback = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank() || line.equals(HEADER)) {
                continue;
            }
            try {
                List<String> cells = CsvCells.parse(line);
                feedback.add(new Feedback(cells.get(1), cells.get(2),
                        MatchStatus.valueOf(cells.get(3)), MatchStatus.valueOf(cells.get(4))));
            } catch (RuntimeException e) {
                LOG.warn("Linha ignorada em {}: formato inesperado", path);
            }
        }
        return feedback;
    }
}
