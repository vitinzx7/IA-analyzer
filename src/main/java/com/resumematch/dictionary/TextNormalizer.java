package com.resumematch.dictionary;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Deixa o texto em minusculas e sem acentos, preservando pontuacao.
 *
 * A pontuacao precisa sobreviver porque C#, C++, .NET e Node.js deixam de
 * existir se ela for removida.
 *
 * O comprimento do texto normalizado NAO acompanha o do original: um acento ja
 * escrito de forma decomposta ("a" seguido de acento combinante) ocupa dois
 * caracteres na entrada e apenas um na saida. Por isso a normalizacao devolve
 * junto um mapa de indices, e quem precisa recortar a evidencia do texto
 * original usa esse mapa em vez de reaproveitar a posicao normalizada.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Texto normalizado mais a correspondencia de cada caractere com o trecho
     * do texto original que o gerou.
     */
    public record Normalized(String value, int[] sourceStart, int[] sourceEnd) {

        public int originalStart(int index) {
            return sourceStart[index];
        }

        public int originalEnd(int indexExclusive) {
            return sourceEnd[indexExclusive - 1];
        }
    }

    public static String normalize(String text) {
        return map(text).value();
    }

    public static Normalized map(String text) {
        if (text == null || text.isEmpty()) {
            return new Normalized("", new int[0], new int[0]);
        }
        StringBuilder value = new StringBuilder(text.length());
        int[] starts = new int[text.length() * 3 + 4];
        int[] ends = new int[starts.length];

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int width = Character.charCount(codePoint);
            String piece = foldOne(codePoint);

            if (piece.isEmpty()) {
                if (!value.isEmpty()) {
                    ends[value.length() - 1] = i + width;
                }
            } else {
                for (int k = 0; k < piece.length(); k++) {
                    value.append(piece.charAt(k));
                    starts[value.length() - 1] = i;
                    ends[value.length() - 1] = i + width;
                }
            }
            i += width;
        }
        return new Normalized(value.toString(),
                Arrays.copyOf(starts, value.length()),
                Arrays.copyOf(ends, value.length()));
    }

    private static String foldOne(int codePoint) {
        String lower = new String(Character.toChars(codePoint)).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{Mn}+", "");
    }
}
