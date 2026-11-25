package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.dictionary.TermOccurrence;

/**
 * Classifica cada requisito pela forca da evidencia no curriculo.
 *
 * MATCH exige o termo numa frase com verbo de acao; PARTIAL aceita o termo em
 * qualquer frase; MISSING significa que o termo nao aparece, ou so aparece
 * negado ("sem Docker", "Nunca utilizei AWS"). Um requisito de alternativas
 * ("Azure / AWS / GCP") e satisfeito por qualquer uma das opcoes; um conjunto
 * ("TCP/IP + DNS + DHCP") e avaliado parte por parte.
 *
 * Cada frase e avaliada uma vez so por analise. Avaliar de novo para cada
 * ocorrencia deixava o tempo quadratico num texto com muitas repeticoes.
 */
public final class RequirementMatcher {

    private final TechnologyDictionary dictionary;

    private record Mention(TermOccurrence occurrence, Sentence sentence, ResumeSection section) {
    }

    private record Found(MatchStatus status, String evidence, ResumeSection section) {
    }

    public RequirementMatcher(TechnologyDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /** Requisitos sem prioridade informada contam como obrigatorios. */
    public List<RequirementMatch> match(List<String> requirements, String resumeText,
            List<Sentence> sentences) {
        if (requirements == null) {
            return List.of();
        }
        return matchRequirements(requirements.stream()
                .map(name -> new JobRequirement(name, RequirementPriority.REQUIRED))
                .toList(), resumeText, sentences);
    }

    public List<RequirementMatch> matchRequirements(List<JobRequirement> requirements,
            String resumeText, List<Sentence> sentences) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }
        String text = resumeText == null ? "" : resumeText;
        List<Mention> mentions = mentions(text, sentences == null ? List.of() : sentences);
        Map<Sentence, Boolean> hasAction = new IdentityHashMap<>();
        List<RequirementMatch> result = new ArrayList<>();
        for (JobRequirement requirement : requirements) {
            if (requirement.isComposite()) {
                result.add(matchComposite(requirement, mentions, hasAction));
                continue;
            }
            Found found = find(RequirementAlternatives.options(requirement.name()), mentions, hasAction);
            result.add(new RequirementMatch(requirement.name(), found.status(), found.evidence(),
                    requirement.priority(), List.of(), found.section()));
        }
        return List.copyOf(result);
    }

    /**
     * Um conjunto so e MATCH quando todas as partes tem frase com acao, e so e
     * MISSING quando nenhuma parte aparece. O resto e PARTIAL. A evidencia e a
     * da primeira parte com MATCH, ou da primeira com PARTIAL.
     */
    private RequirementMatch matchComposite(JobRequirement requirement, List<Mention> mentions,
            Map<Sentence, Boolean> hasAction) {
        List<ComponentMatch> parts = new ArrayList<>();
        List<Found> founds = new ArrayList<>();
        for (String component : requirement.components()) {
            Found found = find(List.of(component), mentions, hasAction);
            founds.add(found);
            parts.add(new ComponentMatch(component, found.status(), found.evidence()));
        }
        boolean allMatch = founds.stream().allMatch(found -> found.status() == MatchStatus.MATCH);
        boolean noneFound = founds.stream().allMatch(found -> found.status() == MatchStatus.MISSING);
        MatchStatus status = allMatch ? MatchStatus.MATCH
                : noneFound ? MatchStatus.MISSING
                : MatchStatus.PARTIAL;
        Found shown = status == MatchStatus.MISSING ? null : founds.stream()
                .filter(found -> found.status() != MatchStatus.MISSING)
                .min(Comparator.comparing(Found::status))
                .orElseThrow();
        return new RequirementMatch(requirement.name(), status,
                shown == null ? null : shown.evidence(), requirement.priority(), parts,
                shown == null ? null : shown.section());
    }

    /**
     * Ocorrencias que valem como evidencia, na ordem do texto. Ficam de fora o
     * termo dentro de e-mail e o termo negado.
     */
    private List<Mention> mentions(String text, List<Sentence> sentences) {
        boolean[] inEmail = PersonalData.emailPositions(text);
        TermNegation negation = new TermNegation(text);
        ResumeSections sections = ResumeSections.read(text);
        List<Mention> mentions = new ArrayList<>();
        for (TermOccurrence occurrence : dictionary.findIn(text)) {
            if (inEmail[occurrence.start()]) {
                continue;
            }
            Sentence sentence = sentenceFor(occurrence, sentences);
            if (!negation.negates(occurrence, sentence)) {
                mentions.add(new Mention(occurrence, sentence, sections.sectionAt(occurrence.start())));
            }
        }
        return mentions;
    }

    private Found find(List<String> options, List<Mention> mentions,
            Map<Sentence, Boolean> hasAction) {
        List<Mention> found = mentions.stream()
                .filter(mention -> options.stream().anyMatch(option ->
                        TechnologyEquivalence.satisfies(option, mention.occurrence().technology())))
                .toList();

        if (found.isEmpty()) {
            return new Found(MatchStatus.MISSING, null, null);
        }

        Mention best = null;
        boolean bestHasAction = false;
        for (Mention mention : found) {
            Sentence sentence = mention.sentence();
            boolean action = sentence != null
                    && hasAction.computeIfAbsent(sentence, s -> ActionVerbs.contains(s.text()));
            if (best == null || better(action, mention.section(), bestHasAction, best.section())) {
                best = mention;
                bestHasAction = action;
            }
            if (bestHasAction && best.section().rank() == 0) {
                break;
            }
        }

        String evidence = best.sentence() != null
                ? best.sentence().text()
                : best.occurrence().matchedText();
        return new Found(bestHasAction ? MatchStatus.MATCH : MatchStatus.PARTIAL,
                evidence(evidence), best.section());
    }

    /**
     * Frase com acao vence frase sem acao; entre duas iguais, vence a secao
     * mais forte (ver {@link ResumeSection}); no empate, fica a primeira.
     */
    private static boolean better(boolean action, ResumeSection section,
            boolean bestAction, ResumeSection bestSection) {
        if (action != bestAction) {
            return action;
        }
        return section.rank() < bestSection.rank();
    }

    /**
     * A evidencia vai para a tela e para o CSV; e-mail e telefone saem daqui
     * ja mascarados.
     */
    private String evidence(String text) {
        return PersonalData.mask(text);
    }

    /**
     * As frases do SentenceSplitter vem em ordem e sem sobreposicao, entao a
     * busca e binaria. A varredura linear fica so para uma lista fora desse
     * formato.
     */
    private Sentence sentenceFor(TermOccurrence occurrence, List<Sentence> sentences) {
        int low = 0;
        int high = sentences.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            Sentence sentence = sentences.get(middle);
            if (occurrence.start() < sentence.start()) {
                high = middle - 1;
            } else if (occurrence.start() >= sentence.end()) {
                low = middle + 1;
            } else {
                return sentence;
            }
        }
        for (Sentence sentence : sentences) {
            if (occurrence.start() >= sentence.start() && occurrence.start() < sentence.end()) {
                return sentence;
            }
        }
        for (Sentence sentence : sentences) {
            if (occurrence.start() < sentence.end() && occurrence.end() > sentence.start()) {
                return sentence;
            }
        }
        return null;
    }
}
