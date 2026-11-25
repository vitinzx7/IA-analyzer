package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.resumematch.dictionary.TextNormalizer;

/**
 * Procura no curriculo a formacao e o idioma que a vaga pede.
 *
 * Nao compara curso nem nivel: so mostra o trecho do curriculo que parece
 * responder, para a pessoa conferir. Nada disso entra no score.
 */
public final class ProfileEvidence {

    private static final Pattern EDUCATION = Pattern.compile(
            "\\b(?:cursando|graduacao|graduad[oa]|bacharelado|bacharel|tecnologo|licenciatura"
                    + "|ensino superior|superior (?:completo|incompleto|em andamento)"
                    + "|universidade|faculdade|centro universitario)\\b");
    private static final List<String> LANGUAGES = List.of(
            "ingles", "espanhol", "frances", "alemao", "italiano", "mandarim", "japones");

    private ProfileEvidence() {
    }

    public static List<UnevaluatedItem> attach(List<UnevaluatedItem> items, String resumeText,
            List<Sentence> sentences) {
        if (items == null || items.isEmpty() || sentences == null || sentences.isEmpty()) {
            return items == null ? List.of() : items;
        }
        ResumeSections sections = ResumeSections.read(resumeText);
        List<UnevaluatedItem> result = new ArrayList<>();
        for (UnevaluatedItem item : items) {
            Sentence found = switch (item.reason()) {
                case EDUCATION -> best(sentences, sections, ResumeSection.EDUCATION, EDUCATION);
                case LANGUAGE -> best(sentences, sections, ResumeSection.LANGUAGES, languages(item.text()));
                case NO_KNOWN_TERM -> null;
            };
            result.add(found == null ? item : item.withResumeEvidence(PersonalData.mask(found.text())));
        }
        return List.copyOf(result);
    }

    /** Idiomas citados no trecho da vaga; se nenhum for citado, qualquer um serve. */
    private static Pattern languages(String jobText) {
        String normalized = TextNormalizer.normalize(jobText);
        List<String> named = LANGUAGES.stream()
                .filter(language -> Pattern.compile("\\b" + language + "\\b").matcher(normalized).find())
                .toList();
        return Pattern.compile("\\b(?:" + String.join("|", named.isEmpty() ? LANGUAGES : named) + ")\\b");
    }

    /** A primeira frase que casa, preferindo a secao propria do assunto. */
    private static Sentence best(List<Sentence> sentences, ResumeSections sections,
            ResumeSection preferred, Pattern pattern) {
        Sentence first = null;
        for (Sentence sentence : sentences) {
            if (!pattern.matcher(TextNormalizer.normalize(sentence.text())).find()) {
                continue;
            }
            if (sections.sectionAt(sentence.start()) == preferred) {
                return sentence;
            }
            if (first == null) {
                first = sentence;
            }
        }
        return first;
    }
}
