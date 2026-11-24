package com.resumematch.matcher;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.resumematch.dictionary.TermOccurrence;
import com.resumematch.dictionary.TextNormalizer;

/**
 * Acha nomes que parecem tecnologia, mas que o dicionario nao reconhece.
 *
 * O sinal e a letra maiuscula no meio da frase: "Jira", "GLPI", "Google
 * Workspace". A primeira palavra do trecho so conta quando nao e so a inicial
 * maiuscula ("PowerShell" conta, "Apoiar" nao). Palavras seguidas com
 * maiuscula viram um nome so. A lista mostra onde o dicionario precisa
 * crescer; ela nao entra no score.
 */
final class UnknownTerms {

    static final int MAX_TERMS = 30;

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}+#.-]*");
    private static final Set<String> IGNORED = Set.of(
            "ti", "t.i", "rh", "clt", "pj", "pcd", "ead", "cnh", "ltda", "ex", "etc",
            "ingles", "espanhol", "frances", "alemao", "italiano", "idioma", "idiomas",
            "brasil", "brasilia");

    private UnknownTerms() {
    }

    /**
     * @param occurrences termos reconhecidos no trecho, que ficam de fora
     * @param found nomes ja achados, pela forma normalizada; recebe os novos
     */
    static void collect(String text, int start, int end, List<TermOccurrence> occurrences,
            Map<String, String> found) {
        Matcher word = WORD.matcher(text).region(start, end);
        StringBuilder run = null;
        int runEnd = -1;
        boolean first = true;
        while (word.find()) {
            String value = trimEnd(word.group());
            int valueStart = word.start();
            int valueEnd = valueStart + value.length();
            boolean candidate = isCandidate(value, first) && !overlaps(occurrences, valueStart, valueEnd);
            first = false;
            if (candidate && run != null && " ".equals(text.substring(runEnd, valueStart))) {
                run.append(' ').append(value);
                runEnd = valueEnd;
                continue;
            }
            add(run, found);
            run = candidate ? new StringBuilder(value) : null;
            runEnd = valueEnd;
        }
        add(run, found);
    }

    private static boolean isCandidate(String value, boolean first) {
        if (value.length() < 2 || !Character.isUpperCase(value.codePointAt(0))) {
            return false;
        }
        boolean onlyInitial = value.substring(1).chars().noneMatch(Character::isUpperCase);
        if (first && onlyInitial) {
            return false;
        }
        return !IGNORED.contains(TextNormalizer.normalize(value));
    }

    private static boolean overlaps(List<TermOccurrence> occurrences, int start, int end) {
        return occurrences.stream().anyMatch(o -> o.start() < end && o.end() > start);
    }

    private static String trimEnd(String value) {
        int end = value.length();
        while (end > 1 && ".-".indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }

    private static void add(StringBuilder run, Map<String, String> found) {
        if (run != null && found.size() < MAX_TERMS) {
            found.putIfAbsent(TextNormalizer.normalize(run.toString()), run.toString());
        }
    }
}
