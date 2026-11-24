package com.resumematch.matcher;

import java.util.regex.Pattern;

import com.resumematch.dictionary.TextNormalizer;

/**
 * Verbos de acao em primeira pessoa do passado.
 *
 * A lista e curta de proposito: ela separa uma frase que conta o que a pessoa
 * fez de uma lista de conhecimentos ou de um verbo de posse.
 */
public final class ActionVerbs {

    private static final Pattern PATTERN = Pattern.compile(
            "\\b(desenvolvi|implementei|criei|publiquei|realizei|utilizei|configurei|apoiei|mantive|escrevi)\\b");

    // A negacao so anula o verbo quando vem logo antes dele, na mesma oracao.
    // Assim "Sem experiencia previa, desenvolvi..." continua valendo.
    private static final Pattern NEGATION = Pattern.compile(
            "\\b(nao|nunca|jamais|sem)\\s*$");

    private ActionVerbs() {
    }

    /**
     * Diz se o texto tem algum verbo da lista, negado ou nao.
     */
    public static boolean mentions(String text) {
        return text != null && PATTERN.matcher(TextNormalizer.normalize(text)).find();
    }

    public static boolean contains(String sentence) {
        if (sentence == null) {
            return false;
        }
        String normalized = TextNormalizer.normalize(sentence);
        var matcher = PATTERN.matcher(normalized);
        while (matcher.find()) {
            int clauseStart = Math.max(normalized.lastIndexOf(',', matcher.start()),
                    normalized.lastIndexOf(';', matcher.start())) + 1;
            String beforeVerb = normalized.substring(clauseStart, matcher.start());
            if (!NEGATION.matcher(beforeVerb).find()) {
                return true;
            }
        }
        return false;
    }
}
