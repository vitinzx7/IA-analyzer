package com.resumematch.matcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.resumematch.dictionary.TermOccurrence;
import com.resumematch.dictionary.TextNormalizer;

/**
 * Decide se uma ocorrencia de termo esta negada e, portanto, nao e evidencia.
 *
 * Duas formas contam, sempre dentro da mesma oracao (entre virgulas ou ponto e
 * virgula):
 * - a negacao vem logo antes do termo, com artigo opcional: "sem Docker",
 *   "sem o Docker";
 * - a oracao tem verbo da lista e todos estao negados: "Nunca utilizei AWS".
 *
 * Nao ha janela de N palavras (D1), entao "sem experiencia em Java" nao e pego.
 * Cada oracao e avaliada uma vez so por texto.
 */
final class TermNegation {

    private static final Set<String> NEGATIONS = Set.of("sem", "nao", "nunca", "jamais");
    private static final Set<String> ARTICLES = Set.of("o", "a", "os", "as");

    private final String text;
    private final int[] separators;
    private final Map<Long, Boolean> negatedClauses = new HashMap<>();

    TermNegation(String text) {
        this.text = text;
        this.separators = clauseSeparators(text);
    }

    boolean negates(TermOccurrence occurrence, Sentence sentence) {
        int from = sentence != null ? sentence.start() : 0;
        int to = sentence != null ? sentence.end() : text.length();
        int clauseStart = Math.max(from, previousSeparator(occurrence.start()) + 1);
        int clauseEnd = Math.min(to, nextSeparator(occurrence.end()));
        if (clauseEnd <= clauseStart) {
            return false;
        }
        return negationRightBefore(clauseStart, occurrence.start())
                || negatedClauses.computeIfAbsent(((long) clauseStart << 32) | clauseEnd,
                        key -> onlyNegatedVerbs(text.substring(clauseStart, clauseEnd)));
    }

    private static boolean onlyNegatedVerbs(String clause) {
        return ActionVerbs.mentions(clause) && !ActionVerbs.contains(clause);
    }

    private boolean negationRightBefore(int limit, int termStart) {
        int[] word = previousWord(limit, termStart);
        if (word != null && ARTICLES.contains(normalized(word))) {
            word = previousWord(limit, word[0]);
        }
        return word != null && NEGATIONS.contains(normalized(word));
    }

    /**
     * A palavra imediatamente antes da posicao, separada dela so por espacos.
     */
    private int[] previousWord(int limit, int position) {
        int end = position;
        while (end > limit && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end == position) {
            return null;
        }
        int start = end;
        while (start > limit && isWordChar(text.charAt(start - 1))) {
            start--;
        }
        return start < end ? new int[] { start, end } : null;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetter(c) || Character.getType(c) == Character.NON_SPACING_MARK;
    }

    private String normalized(int[] word) {
        return TextNormalizer.normalize(text.substring(word[0], word[1]));
    }

    private int previousSeparator(int position) {
        int index = Arrays.binarySearch(separators, position);
        int insertion = index >= 0 ? index : -index - 1;
        return insertion > 0 ? separators[insertion - 1] : -1;
    }

    private int nextSeparator(int position) {
        int index = Arrays.binarySearch(separators, position);
        int insertion = index >= 0 ? index : -index - 1;
        return insertion < separators.length ? separators[insertion] : text.length();
    }

    private static int[] clauseSeparators(String text) {
        int count = 0;
        int[] positions = new int[16];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ',' || c == ';') {
                if (count == positions.length) {
                    positions = Arrays.copyOf(positions, count * 2);
                }
                positions[count++] = i;
            }
        }
        return Arrays.copyOf(positions, count);
    }
}
