package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simula quanto a cobertura subiria com cada mudanca possivel no curriculo.
 *
 * A conta usa o mesmo {@link ScoreCalculator} da cobertura real, rodando sobre
 * uma copia da lista de requisitos com a mudanca aplicada. A lista recebida
 * nunca e alterada. So entram duas mudancas:
 *
 * <pre>
 * LIST_ONLY  requisito PARTIAL descrito numa frase de acao  -> MATCH
 * INDICATED  requisito MISSING indicado por outro termo     -> MATCH
 * </pre>
 *
 * Requisito MISSING sem indicio vira lacuna real e nao da pontos: o curriculo
 * nao tem nada que sugira que a pessoa tenha aquilo.
 */
public final class ImprovementSimulator {

    private final RequirementMatcher matcher;
    private final ScoreCalculator scoreCalculator;

    public ImprovementSimulator(RequirementMatcher matcher, ScoreCalculator scoreCalculator) {
        this.matcher = matcher;
        this.scoreCalculator = scoreCalculator;
    }

    public ImprovementSimulation simulate(List<RequirementMatch> requirements, String resumeText,
            List<Sentence> sentences) {
        if (requirements == null || requirements.isEmpty()) {
            return ImprovementSimulation.EMPTY;
        }
        List<RequirementMatch> original = List.copyOf(requirements);
        double realScore = scoreCalculator.calculate(original);
        Map<String, RequirementMatch> indicators = indicatorsInResume(original, resumeText, sentences);

        List<RequirementMatch> allChanges = new ArrayList<>(original);
        List<ImprovementSuggestion> suggestions = new ArrayList<>();
        List<RealGap> gaps = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            RequirementMatch requirement = original.get(i);
            if (requirement.status() == MatchStatus.MATCH) {
                continue;
            }
            List<String> missingParts = missingParts(requirement);
            RequirementMatch indicator = requirement.status() == MatchStatus.MISSING
                    ? firstIndicator(requirement, indicators)
                    : null;
            // Num conjunto com parte ausente, escrever melhor o que ja existe
            // nao vira MATCH: a parte que falta continua faltando.
            boolean listOnly = requirement.status() == MatchStatus.PARTIAL && missingParts.isEmpty();
            if (!listOnly && indicator == null) {
                gaps.add(new RealGap(requirement.name(), requirement.priority(), missingParts));
                continue;
            }

            String evidence = listOnly ? requirement.evidence() : indicator.evidence();
            RequirementMatch changed = new RequirementMatch(requirement.name(), MatchStatus.MATCH,
                    evidence, requirement.priority(), requirement.components(),
                    listOnly ? requirement.evidenceSection() : indicator.evidenceSection());
            List<RequirementMatch> onlyThis = new ArrayList<>(original);
            onlyThis.set(i, changed);
            allChanges.set(i, changed);

            double scoreWithChange = scoreCalculator.calculate(onlyThis);
            suggestions.add(new ImprovementSuggestion(requirement.name(),
                    listOnly ? SuggestionType.LIST_ONLY : SuggestionType.INDICATED,
                    listOnly ? null : indicator.name(), evidence, requirement.priority(),
                    scoreWithChange, roundOneDecimal(scoreWithChange - realScore)));
        }
        suggestions.sort(Comparator.comparingDouble(ImprovementSuggestion::points).reversed());
        Double scoreWithAll = suggestions.isEmpty() ? null : scoreCalculator.calculate(allChanges);
        return new ImprovementSimulation(suggestions, scoreWithAll, gaps);
    }

    /**
     * Procura no curriculo, com o mesmo matcher da analise, so os termos que
     * indicam algum requisito ausente. Termo negado ou dentro de e-mail nao
     * conta, como na cobertura real.
     */
    private Map<String, RequirementMatch> indicatorsInResume(List<RequirementMatch> requirements,
            String resumeText, List<Sentence> sentences) {
        Set<String> wanted = new LinkedHashSet<>();
        for (RequirementMatch requirement : requirements) {
            if (requirement.status() == MatchStatus.MISSING) {
                wanted.addAll(ImprovementHints.indicatorsOf(requirement.name()));
            }
        }
        if (wanted.isEmpty()) {
            return Map.of();
        }
        List<JobRequirement> search = wanted.stream()
                .map(name -> new JobRequirement(name, RequirementPriority.REQUIRED))
                .toList();
        Map<String, RequirementMatch> found = new LinkedHashMap<>();
        for (RequirementMatch match : matcher.matchRequirements(search, resumeText, sentences)) {
            if (match.status() != MatchStatus.MISSING) {
                found.put(match.name(), match);
            }
        }
        return found;
    }

    private static RequirementMatch firstIndicator(RequirementMatch requirement,
            Map<String, RequirementMatch> indicators) {
        return ImprovementHints.indicatorsOf(requirement.name()).stream()
                .map(indicators::get)
                .filter(match -> match != null)
                .findFirst()
                .orElse(null);
    }

    private static List<String> missingParts(RequirementMatch requirement) {
        return requirement.components().stream()
                .filter(component -> component.status() == MatchStatus.MISSING)
                .map(ComponentMatch::name)
                .toList();
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
