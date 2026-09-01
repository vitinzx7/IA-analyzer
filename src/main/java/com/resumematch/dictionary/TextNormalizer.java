package com.resumematch.dictionary;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Deixa o texto em minusculas e sem acentos, preservando pontuacao.
 *
 * A pontuacao precisa sobreviver porque C#, C++, .NET e Node.js deixam de
 * existir se ela for removida. O comprimento do texto tambem e preservado, de
 * modo que uma posicao no texto normalizado aponta para o mesmo caractere no
 * texto original, e a evidencia pode ser recortada do original, legivel.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}+", "");
    }
}
