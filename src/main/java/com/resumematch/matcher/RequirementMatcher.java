package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.dictionary.TermOccurrence;

/**
 * Classifica cada requisito pela forca da evidencia no curriculo.
 *
 * MATCH exige o termo numa frase com verbo de acao; PARTIAL aceita o termo em
 * qualquer frase; MISSING significa que o termo nao aparece. Um requisito de
 * alternativas ("Azure / AWS / GCP") e satisfeito por qualquer uma das opcoes.
 */
public final class RequirementMatcher {

    private final TechnologyDictionary dictionary;

    public RequirementMatcher(TechnologyDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<RequirementMatch> match(List<String> requirements, String resumeText,
            List<Sentence> sentences) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }
        List<TermOccurrence> occurrences = dictionary.findIn(resumeText);
        List<Sentence> safeSentences = sentences == null ? List.of() : sentences;
        List<RequirementMatch> result = new ArrayList<>();
        for (String requirement : requirements) {
            result.add(matchOne(requirement, occurrences, safeSentences));
        }
        return List.copyOf(result);
    }

    private RequirementMatch matchOne(String requirement, List<TermOccurrence> occurrences,
            List<Sentence> sentences) {
        List<String> options = RequirementAlternatives.options(requirement);
        List<TermOccurrence> found = occurrences.stream()
                .filter(occurrence -> options.stream()
                        .anyMatch(option -> TechnologyEquivalence.satisfies(option, occurrence.technology())))
                .sorted(Comparator.comparingInt(TermOccurrence::start))
                .toList();

        if (found.isEmpty()) {
            return new RequirementMatch(requirement, MatchStatus.MISSING, null);
        }

        for (TermOccurrence occurrence : found) {
            Sentence sentence = sentenceFor(occurrence, sentences);
            if (sentence != null && ActionVerbs.contains(sentence.text())) {
                return new RequirementMatch(requirement, MatchStatus.MATCH, sentence.text());
            }
        }

        Sentence sentence = sentenceFor(found.getFirst(), sentences);
        String evidence = sentence != null ? sentence.text() : found.getFirst().matchedText();
        return new RequirementMatch(requirement, MatchStatus.PARTIAL, evidence);
    }

    private Sentence sentenceFor(TermOccurrence occurrence, List<Sentence> sentences) {
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
