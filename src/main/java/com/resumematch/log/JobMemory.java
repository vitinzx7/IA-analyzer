package com.resumematch.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.resumematch.analysis.AnalysisLog;
import com.resumematch.analysis.JobHistory;
import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.MatchStatus;
import com.resumematch.matcher.PreviousAnalysis;
import com.resumematch.matcher.RequirementMatch;
import com.resumematch.matcher.RequirementPriority;

/**
 * Memoria de vagas: uma linha por analise num CSV local, com a vaga, a nota e
 * os requisitos separados por prioridade e status.
 *
 * Nao grava trecho do curriculo, so o que veio da vaga e a decisao do
 * programa. A mesma vaga analisada de novo ganha outra linha com a mesma
 * chave, para dar para ver a nota mudar depois que o curriculo muda. Quem le
 * os padroes conta so a linha mais recente de cada chave.
 *
 * Gravar e ler ficam na mesma classe para usar a mesma trava: a leitura nunca
 * pega uma linha pela metade.
 */
public final class JobMemory implements AnalysisLog, JobHistory {

    static final String HEADER = "data,chave,cargo,empresa,local,link,cobertura,"
            + "obrigatorios_atendidos,obrigatorios_parciais,obrigatorios_faltando,"
            + "diferenciais_atendidos,diferenciais_parciais,diferenciais_faltando,"
            + "termos_desconhecidos";
    static final String LIST_SEPARATOR = "; ";

    private static final Logger LOG = LoggerFactory.getLogger(JobMemory.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<RequirementPriority> PRIORITIES =
            List.of(RequirementPriority.REQUIRED, RequirementPriority.PREFERRED);
    private static final List<MatchStatus> STATUSES =
            List.of(MatchStatus.MATCH, MatchStatus.PARTIAL, MatchStatus.MISSING);

    // Posicao das colunas, na ordem do HEADER.
    private static final int COLUMNS = HEADER.split(",").length;
    private static final int DATE = 0;
    private static final int KEY = 1;
    private static final int COVERAGE = 6;
    private static final int FIRST_REQUIRED = 7;
    private static final int FIRST_PREFERRED = 10;
    private static final int UNKNOWN_TERMS = 13;

    private final Path path;
    private final boolean enabled;

    public JobMemory(Path path, boolean enabled) {
        this.path = path;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
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

    @Override
    public synchronized PreviousAnalysis previous(String jobText) {
        if (!enabled) {
            return null;
        }
        try {
            String key = JobHeader.read(jobText).key();
            List<List<String>> same = rows().stream().filter(row -> row.get(KEY).equals(key)).toList();
            if (same.isEmpty()) {
                return null;
            }
            List<String> last = same.getLast();
            return new PreviousAnalysis(same.size(), last.get(DATE), coverage(last.get(COVERAGE)));
        } catch (Exception e) {
            LOG.warn("Nao foi possivel ler a memoria de vagas em {}", path, e);
            return null;
        }
    }

    public synchronized JobPatterns patterns() {
        if (!enabled) {
            return JobPatterns.disabled();
        }
        List<List<String>> rows;
        try {
            rows = rows();
        } catch (IOException e) {
            LOG.warn("Nao foi possivel ler a memoria de vagas em {}", path, e);
            rows = List.of();
        }
        Map<String, List<String>> latest = new LinkedHashMap<>();
        rows.forEach(row -> latest.put(row.get(KEY), row));

        Map<String, int[]> requirements = new HashMap<>();
        Map<String, Integer> unknown = new HashMap<>();
        for (List<String> row : latest.values()) {
            Set<String> required = items(row, FIRST_REQUIRED);
            Set<String> preferred = items(row, FIRST_PREFERRED);
            preferred.removeAll(required);
            required.forEach(name -> requirements.computeIfAbsent(name, n -> new int[2])[0]++);
            preferred.forEach(name -> requirements.computeIfAbsent(name, n -> new int[2])[1]++);
            split(row.get(UNKNOWN_TERMS)).forEach(name -> unknown.merge(name, 1, Integer::sum));
        }

        List<JobPatterns.Requirement> ranked = requirements.entrySet().stream()
                .map(e -> new JobPatterns.Requirement(e.getKey(), e.getValue()[0] + e.getValue()[1],
                        e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingInt(JobPatterns.Requirement::jobs).reversed()
                        .thenComparing(JobPatterns.Requirement::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<JobPatterns.Term> terms = unknown.entrySet().stream()
                .map(e -> new JobPatterns.Term(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(JobPatterns.Term::jobs).reversed()
                        .thenComparing(JobPatterns.Term::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new JobPatterns(true, latest.size(), rows.size(), ranked, terms);
    }

    /** Linhas com o numero certo de colunas; o resto e pulado com aviso. */
    private List<List<String>> rows() throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        List<List<String>> rows = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.equals(HEADER)) {
                continue;
            }
            List<String> cells = CsvCells.parse(line);
            if (cells.size() == COLUMNS && !cells.get(KEY).isBlank()) {
                rows.add(cells);
            } else {
                LOG.warn("Linha ignorada em {}: formato inesperado", path);
            }
        }
        return rows;
    }

    /** Os nomes das tres colunas de status de uma prioridade. */
    private static Set<String> items(List<String> row, int first) {
        Set<String> names = new LinkedHashSet<>();
        for (int column = first; column < first + STATUSES.size(); column++) {
            names.addAll(split(row.get(column)));
        }
        return names;
    }

    private static List<String> split(String cell) {
        return Arrays.stream(cell.split(";"))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private static Double coverage(String cell) {
        try {
            return cell.isBlank() ? null : Double.valueOf(cell);
        } catch (NumberFormatException e) {
            return null;
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
