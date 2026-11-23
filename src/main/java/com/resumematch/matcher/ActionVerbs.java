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

    private ActionVerbs() {
    }

    public static boolean contains(String sentence) {
        return sentence != null && PATTERN.matcher(TextNormalizer.normalize(sentence)).find();
    }
}
