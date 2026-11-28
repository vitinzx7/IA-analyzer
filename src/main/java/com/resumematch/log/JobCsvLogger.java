package com.resumematch.log;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.resumematch.analysis.AnalysisLog;
import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.MatchStatus;
import com.resumematch.matcher.RequirementMatch;
import com.resumematch.matcher.RequirementPriority;

/**
 * Memoria de vagas: uma linha por analise num CSV local, com a vaga, a nota e
 * os requisitos separados por prioridade e status.
 *
 * Nao grava trecho do curriculo, so o que veio da vaga e a decisao do
 * programa. A mesma vaga analisada de novo ganha outra linha com a mesma
 * chave, para dar para ver a nota mudar depois que o curriculo muda.
 */
public final class JobCsvLogger implements AnalysisLog {

    static final String HEADER = "data,chave,cargo,empresa,local,link,cobertura,"
            + "obrigatorios_atendidos,obrigatorios_parciais,obrigatorios_faltando,"
            + "diferenciais_atendidos,diferenciais_parciais,diferenciais_faltando,"
            + "termos_desconhecidos";
    static final String LIST_SEPARATOR = "; ";

    private static final Logger LOG = LoggerFactory.getLogger(JobCsvLogger.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<RequirementPriority> PRIORITIES =
            List.of(RequirementPriority.REQUIRED, RequirementPriority.PREFERRED);
    private static final List<MatchStatus> STATUSES =
            List.of(MatchStatus.MATCH, MatchStatus.PARTIAL, MatchStatus.MISSING);

    private final Path path;
    private final boolean enabled;

    public JobCsvLogger(Path path, boolean enabled) {
        this.path = path;
        this.enabled = enabled;
    }

    @Override
    public synchronized void log(AnalysisResult result, String jobText) {
        if (!enabled || result == null) {
            return;
        }
        try {
            JobHeader job = JobHeader.read(jobText);
            List<String> cells = new ArrayList<>(List.of(
                    LocalDateTime.now().format(DATE_TIME),
                    job.key(), job.title(), job.company(), job.location(), job.link(),
                    result.score() == null ? "" : result.score().toString()));
            for (RequirementPriority priority : PRIORITIES) {
                for (MatchStatus status : STATUSES) {
                    cells.add(names(result.requirements(), priority, status));
                }
            }
            cells.add(list(result.unknownTerms()));
            String row = cells.stream().map(CsvCells::cell).collect(Collectors.joining(",")) + "\n";
            CsvFiles.append(path, HEADER, row);
        } catch (Exception e) {
            LOG.warn("Nao foi possivel gravar a memoria de vagas em {}", path, e);
        }
    }

    private static String names(List<RequirementMatch> requirements, RequirementPriority priority,
            MatchStatus status) {
        return list(requirements.stream()
                .filter(requirement -> requirement.priority() == priority && requirement.status() == status)
                .map(RequirementMatch::name)
                .toList());
    }

    // O ";" separa os itens da celula, entao nao pode aparecer dentro de um nome.
    private static String list(List<String> names) {
        Set<String> unique = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                unique.add(name.replace(';', ',').strip());
            }
        }
        return String.join(LIST_SEPARATOR, unique);
    }
}
