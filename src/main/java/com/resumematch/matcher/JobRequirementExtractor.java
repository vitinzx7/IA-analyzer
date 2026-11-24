package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.dictionary.TermOccurrence;

/**
 * Extrai os requisitos reconheciveis de uma descricao de vaga.
 *
 * O dicionario e a fonte da verdade: termos fora dele nao entram no calculo.
 *
 * A vaga e lida por secao (ver {@link JobSegments}): o que esta em
 * "Diferenciais", ou num trecho com "desejavel", vira requisito PREFERRED; o
 * que esta em beneficios, local ou horario nao vira requisito.
 *
 * Uma lista que termina em "ou" (ou ligada por "/") pode virar um unico
 * requisito de alternativas, como "Azure / AWS / GCP". So o trecho final da
 * lista que for todo da mesma familia e agrupado; o resto continua separado.
 * Em "Java, Docker, Azure, AWS ou GCP" saem Java, Docker e o grupo de nuvem.
 */
public final class JobRequirementExtractor {

    private static final Pattern COMMA_GAP = Pattern.compile("\\s*,\\s*");
    private static final Pattern OR_GAP = Pattern.compile(
            "\\s*,?\\s*(?:ou|e/ou)\\s*|\\s*/\\s*", Pattern.CASE_INSENSITIVE);

    private enum Gap { COMMA, OR, OTHER }

    private record Item(List<String> technologies, RequirementPriority priority) {
    }

    private final TechnologyDictionary dictionary;

    public JobRequirementExtractor(TechnologyDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /** Nomes dos requisitos, na ordem em que aparecem na vaga. */
    public List<String> extract(String jobText) {
        return requirements(jobText).stream().map(JobRequirement::name).toList();
    }

    public List<JobRequirement> requirements(String jobText) {
        if (jobText == null || jobText.isBlank()) {
            return List.of();
        }
        List<TermOccurrence> occurrences = dictionary.findIn(jobText);
        List<Item> items = new ArrayList<>();
        int next = 0;
        for (JobSegments.Segment segment : JobSegments.read(jobText, occurrences)) {
            while (next < occurrences.size() && occurrences.get(next).start() < segment.start()) {
                next++;
            }
            int first = next;
            while (next < occurrences.size() && occurrences.get(next).end() <= segment.end()) {
                next++;
            }
            if (segment.section() != JobSection.IGNORED && first < next) {
                addSegment(jobText, occurrences.subList(first, next), segment.priority(), items);
            }
        }
        return requirements(items);
    }

    private void addSegment(String jobText, List<TermOccurrence> occurrences,
            RequirementPriority priority, List<Item> items) {
        int runStart = 0;
        for (int i = 1; i <= occurrences.size(); i++) {
            if (i == occurrences.size() || gap(jobText, occurrences, i) == Gap.OTHER) {
                addList(jobText, occurrences.subList(runStart, i), priority, items);
                runStart = i;
            }
        }
    }

    /**
     * Recebe termos ligados so por virgula, "ou" ou "/". Cada "ou" fecha um
     * candidato a grupo com os termos acumulados desde o fechamento anterior.
     */
    private void addList(String jobText, List<TermOccurrence> list, RequirementPriority priority,
            List<Item> items) {
        int segmentStart = 0;
        int k = 1;
        while (k < list.size()) {
            if (gap(jobText, list, k) != Gap.OR) {
                k++;
                continue;
            }
            int end = k;
            while (end + 1 < list.size() && gap(jobText, list, end + 1) == Gap.OR) {
                end++;
            }
            addCandidate(technologies(list, segmentStart, end + 1), priority, items);
            segmentStart = end + 1;
            k = end + 2;
        }
        for (int i = segmentStart; i < list.size(); i++) {
            items.add(new Item(List.of(list.get(i).technology()), priority));
        }
    }

    private void addCandidate(List<String> terms, RequirementPriority priority, List<Item> items) {
        int groupStart = terms.size();
        for (int start = 0; start < terms.size() - 1; start++) {
            if (TechnologyFamilies.sameFamily(distinct(terms.subList(start, terms.size())))) {
                groupStart = start;
                break;
            }
        }
        for (int i = 0; i < groupStart; i++) {
            items.add(new Item(List.of(terms.get(i)), priority));
        }
        if (groupStart < terms.size()) {
            items.add(new Item(distinct(terms.subList(groupStart, terms.size())), priority));
        }
    }

    /**
     * Uma tecnologia que ja esta num grupo nao entra de novo sozinha, mesmo que
     * apareca antes do grupo no texto da vaga. O requisito que aparece em mais
     * de um trecho fica com a maior prioridade entre eles.
     */
    private List<JobRequirement> requirements(List<Item> items) {
        Set<String> grouped = new HashSet<>();
        items.stream().filter(item -> item.technologies().size() > 1)
                .forEach(item -> grouped.addAll(item.technologies()));

        Map<String, RequirementPriority> requirements = new LinkedHashMap<>();
        for (Item item : items) {
            List<String> technologies = item.technologies();
            if (technologies.size() > 1) {
                requirements.merge(RequirementAlternatives.join(technologies), item.priority(),
                        RequirementPriority::highest);
            } else if (!grouped.contains(technologies.getFirst())) {
                requirements.merge(technologies.getFirst(), item.priority(),
                        RequirementPriority::highest);
            }
        }
        return requirements.entrySet().stream()
                .map(entry -> new JobRequirement(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Gap gap(String jobText, List<TermOccurrence> occurrences, int index) {
        String between = jobText.substring(
                occurrences.get(index - 1).end(), occurrences.get(index).start());
        if (COMMA_GAP.matcher(between).matches()) {
            return Gap.COMMA;
        }
        if (OR_GAP.matcher(between).matches()) {
            return Gap.OR;
        }
        return Gap.OTHER;
    }

    private static List<String> technologies(List<TermOccurrence> list, int from, int to) {
        return list.subList(from, to).stream().map(TermOccurrence::technology).toList();
    }

    private static List<String> distinct(List<String> terms) {
        return List.copyOf(new LinkedHashSet<>(terms));
    }
}
