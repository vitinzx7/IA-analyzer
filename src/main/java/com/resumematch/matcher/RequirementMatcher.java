package com.resumematch.matcher;

import java.util.ArrayList;
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
 * ("Azure / AWS / GCP") e satisfeito por qualquer uma das opcoes.
 *
 * Cada frase e avaliada uma vez so por analise. Avaliar de novo para cada
 * ocorrencia deixava o tempo quadratico num texto com muitas repeticoes.
 */
public final class RequirementMatcher {

    private final TechnologyDictionary dictionary;

    private record Mention(TermOccurrence occurrence, Sentence sentence) {
    }

    public RequirementMatcher(TechnologyDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<RequirementMatch> match(List<String> requirements, String resumeText,
            List<Sentence> sentences) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }
        String text = resumeText == null ? "" : resumeText;
        List<Mention> mentions = mentions(text, sentences == null ? List.of() : sentences);
        Map<Sentence, Boolean> hasAction = new IdentityHashMap<>();
        List<RequirementMatch> result = new ArrayList<>();
        for (String requirement : requirements) {
            result.add(matchOne(requirement, mentions, hasAction));
        }
        return List.copyOf(result);
    }

    /**
     * Ocorrencias que valem como evidencia, na ordem do texto. Ficam de fora o
     * termo dentro de e-mail e o termo negado.
     */
    private List<Mention> mentions(String text, List<Sentence> sentences) {
        boolean[] inEmail = PersonalData.emailPositions(text);
        TermNegation negation = new TermNegation(text);
        List<Mention> mentions = new ArrayList<>();
        for (TermOccurrence occurrence : dictionary.findIn(text)) {
            if (inEmail[occurrence.start()]) {
                continue;
            }
            Sentence sentence = sentenceFor(occurrence, sentences);
            if (!negation.negates(occurrence, sentence)) {
                mentions.add(new Mention(occurrence, sentence));
            }
        }
        return mentions;
    }

    private RequirementMatch matchOne(String requirement, List<Mention> mentions,
            Map<Sentence, Boolean> hasAction) {
        List<String> options = RequirementAlternatives.options(requirement);
        List<Mention> found = mentions.stream()
                .filter(mention -> options.stream().anyMatch(option ->
                        TechnologyEquivalence.satisfies(option, mention.occurrence().technology())))
                .toList();

        if (found.isEmpty()) {
            return new RequirementMatch(requirement, MatchStatus.MISSING, null);
        }

        for (Mention mention : found) {
            Sentence sentence = mention.sentence();
            if (sentence != null
                    && hasAction.computeIfAbsent(sentence, s -> ActionVerbs.contains(s.text()))) {
                return new RequirementMatch(requirement, MatchStatus.MATCH, evidence(sentence.text()));
            }
        }

        Mention first = found.getFirst();
        String evidence = first.sentence() != null
                ? first.sentence().text()
                : first.occurrence().matchedText();
        return new RequirementMatch(requirement, MatchStatus.PARTIAL, evidence(evidence));
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
