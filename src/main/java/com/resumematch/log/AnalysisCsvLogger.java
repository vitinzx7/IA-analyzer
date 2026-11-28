package com.resumematch.log;

import java.nio.file.Path;
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
    public synchronized void log(AnalysisResult result, String jobText) {
        if (!enabled || result == null) {
            return;
        }
        try {
            String date = LocalDate.now().toString();
            StringBuilder rows = new StringBuilder();
            for (RequirementMatch requirement : result.requirements()) {
                rows.append(csv(date)).append(',')
                        .append(csv(requirement.name())).append(',')
                        .append(csv(requirement.status().name())).append(',')
                        .append(csv(requirement.evidence())).append('\n');
            }
            CsvFiles.append(path, HEADER, rows.toString());
        } catch (Exception e) {
            LOG.warn("Nao foi possivel gravar o CSV de analises em {}", path, e);
        }
    }

    private String csv(String value) {
        return CsvCells.cell(value);
    }
}
