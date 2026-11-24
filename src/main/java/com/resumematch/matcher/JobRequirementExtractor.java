package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.dictionary.TermOccurrence;

/**
 * Extrai os requisitos reconheciveis de uma descricao de vaga.
 *
 * O dicionario e a fonte da verdade: termos fora dele nao entram no calculo.
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

    private final TechnologyDictionary dictionary;

    public JobRequirementExtractor(TechnologyDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<String> extract(String jobText) {
        if (jobText == null || jobText.isBlank()) {
            return List.of();
        }
        List<TermOccurrence> occurrences = dictionary.findIn(jobText);
        List<List<String>> items = new ArrayList<>();
        int runStart = 0;
        for (int i = 1; i <= occurrences.size(); i++) {
            if (i == occurrences.size() || gap(jobText, occurrences, i) == Gap.OTHER) {
                addList(jobText, occurrences.subList(runStart, i), items);
                runStart = i;
            }
        }
        return requirements(items);
    }

    /**
     * Recebe termos ligados so por virgula, "ou" ou "/". Cada "ou" fecha um
     * candidato a grupo com os termos acumulados desde o fechamento anterior.
     */
    private void addList(String jobText, List<TermOccurrence> list, List<List<String>> items) {
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
            addCandidate(technologies(list, segmentStart, end + 1), items);
            segmentStart = end + 1;
            k = end + 2;
        }
        for (int i = segmentStart; i < list.size(); i++) {
            items.add(List.of(list.get(i).technology()));
        }
    }

    private void addCandidate(List<String> terms, List<List<String>> items) {
        int groupStart = terms.size();
        for (int start = 0; start < terms.size() - 1; start++) {
            if (TechnologyFamilies.sameFamily(distinct(terms.subList(start, terms.size())))) {
                groupStart = start;
                break;
            }
        }
        for (int i = 0; i < groupStart; i++) {
            items.add(List.of(terms.get(i)));
        }
        if (groupStart < terms.size()) {
            items.add(distinct(terms.subList(groupStart, terms.size())));
        }
    }

    /**
     * Uma tecnologia que ja esta num grupo nao entra de novo sozinha, mesmo que
     * apareca antes do grupo no texto da vaga.
     */
    private List<String> requirements(List<List<String>> items) {
        Set<String> grouped = new HashSet<>();
        items.stream().filter(item -> item.size() > 1).forEach(grouped::addAll);

        Set<String> requirements = new LinkedHashSet<>();
        for (List<String> item : items) {
            if (item.size() > 1) {
                requirements.add(RequirementAlternatives.join(item));
            } else if (!grouped.contains(item.getFirst())) {
                requirements.add(item.getFirst());
            }
        }
        return List.copyOf(requirements);
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
