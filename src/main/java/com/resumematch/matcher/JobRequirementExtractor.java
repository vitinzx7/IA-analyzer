package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.dictionary.TermOccurrence;
import com.resumematch.dictionary.TextNormalizer;

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
 *
 * Dois ou mais termos dentro do mesmo parentese viram um requisito so, para
 * que "noções de redes (TCP/IP, DNS, DHCP)" nao pese como tres. Com "ex.",
 * "como", "etc." ou "similares" no parentese, a lista e de exemplos e vira
 * alternativas; sem isso, vira um conjunto (ver {@link JobRequirement}).
 */
public final class JobRequirementExtractor {

    private static final Pattern COMMA_GAP = Pattern.compile("\\s*,\\s*");
    private static final Pattern OR_GAP = Pattern.compile(
            "\\s*,?\\s*(?:ou|e/ou)\\s*|\\s*/\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARENTHESES = Pattern.compile("\\(([^()]*)\\)");
    private static final Pattern EXAMPLE_CUE = Pattern.compile(
            "\\b(?:ex|exemplo|exemplos|como|similar|similares|etc|outr[oa]s|afins|equivalentes?)\\b");

    private enum Gap { COMMA, OR, OTHER }

    private enum Kind { SINGLE, ANY, ALL }

    private record Item(List<String> technologies, RequirementPriority priority, Kind kind,
            int position) {
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
                addSegment(jobText, segment, occurrences.subList(first, next), items);
            }
        }
        return requirements(items);
    }

    private void addSegment(String jobText, JobSegments.Segment segment,
            List<TermOccurrence> occurrences, List<Item> items) {
        RequirementPriority priority = segment.priority();
        List<Item> segmentItems = new ArrayList<>();
        List<TermOccurrence> loose = new ArrayList<>(occurrences);

        Matcher parentheses = PARENTHESES.matcher(jobText).region(segment.start(), segment.end());
        while (parentheses.find()) {
            int open = parentheses.start();
            int close = parentheses.end();
            List<TermOccurrence> inside = occurrences.stream()
                    .filter(occurrence -> occurrence.start() > open && occurrence.end() < close)
                    .toList();
            List<String> technologies = distinct(technologies(inside));
            if (technologies.size() < 2) {
                continue;
            }
            if (EXAMPLE_CUE.matcher(TextNormalizer.normalize(parentheses.group(1))).find()) {
                segmentItems.add(new Item(technologies, priority, Kind.ANY, open));
            } else if (TechnologyFamilies.sameFamily(technologies) && hasOrGap(jobText, inside)) {
                continue;
            } else {
                segmentItems.add(new Item(technologies, priority, Kind.ALL, open));
            }
            loose.removeAll(inside);
        }

        int runStart = 0;
        for (int i = 1; i <= loose.size(); i++) {
            if (i == loose.size() || gap(jobText, loose, i) == Gap.OTHER) {
                addList(jobText, loose.subList(runStart, i), priority, segmentItems);
                runStart = i;
            }
        }
        segmentItems.sort(Comparator.comparingInt(Item::position));
        items.addAll(segmentItems);
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
            addCandidate(list.subList(segmentStart, end + 1), priority, items);
            segmentStart = end + 1;
            k = end + 2;
        }
        for (int i = segmentStart; i < list.size(); i++) {
            items.add(single(list.get(i), priority));
        }
    }

    private void addCandidate(List<TermOccurrence> terms, RequirementPriority priority,
            List<Item> items) {
        int groupStart = terms.size();
        for (int start = 0; start < terms.size() - 1; start++) {
            if (TechnologyFamilies.sameFamily(distinct(technologies(terms.subList(start, terms.size()))))) {
                groupStart = start;
                break;
            }
        }
        for (int i = 0; i < groupStart; i++) {
            items.add(single(terms.get(i), priority));
        }
        if (groupStart < terms.size()) {
            List<TermOccurrence> group = terms.subList(groupStart, terms.size());
            items.add(new Item(distinct(technologies(group)), priority, Kind.ANY,
                    group.getFirst().start()));
        }
    }

    /**
     * Uma tecnologia que ja esta num grupo ou conjunto nao entra de novo
     * sozinha, mesmo que apareca antes dele no texto da vaga. O requisito que
     * aparece em mais de um trecho fica com a maior prioridade entre eles.
     */
    private List<JobRequirement> requirements(List<Item> items) {
        Set<String> grouped = new HashSet<>();
        items.stream().filter(item -> item.kind() != Kind.SINGLE)
                .forEach(item -> grouped.addAll(item.technologies()));

        Map<String, JobRequirement> requirements = new LinkedHashMap<>();
        for (Item item : items) {
            JobRequirement requirement = switch (item.kind()) {
                case ALL -> JobRequirement.composite(item.technologies(), item.priority());
                case ANY -> new JobRequirement(
                        RequirementAlternatives.join(item.technologies()), item.priority());
                case SINGLE -> grouped.contains(item.technologies().getFirst())
                        ? null
                        : new JobRequirement(item.technologies().getFirst(), item.priority());
            };
            if (requirement != null) {
                requirements.merge(requirement.name(), requirement, (kept, other) ->
                        new JobRequirement(kept.name(),
                                RequirementPriority.highest(kept.priority(), other.priority()),
                                kept.components()));
            }
        }
        return List.copyOf(requirements.values());
    }

    private boolean hasOrGap(String jobText, List<TermOccurrence> occurrences) {
        for (int i = 1; i < occurrences.size(); i++) {
            if (gap(jobText, occurrences, i) == Gap.OR) {
                return true;
            }
        }
        return false;
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

    private static Item single(TermOccurrence occurrence, RequirementPriority priority) {
        return new Item(List.of(occurrence.technology()), priority, Kind.SINGLE, occurrence.start());
    }

    private static List<String> technologies(List<TermOccurrence> occurrences) {
        return occurrences.stream().map(TermOccurrence::technology).toList();
    }

    private static List<String> distinct(List<String> terms) {
        return List.copyOf(new LinkedHashSet<>(terms));
    }
}
