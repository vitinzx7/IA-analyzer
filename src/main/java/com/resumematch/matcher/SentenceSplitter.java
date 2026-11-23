package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide o texto do curriculo em frases.
 *
 * O texto de PDF vem quebrado por wrap visual. A primeira passada remonta os
 * paragrafos; a segunda separa as frases por pontuacao. Cada item de bullet
 * conta como frase propria.
 */
public final class SentenceSplitter {

    private static final String BULLETS = "•-*–—▪◦·●○■□";

    public List<Sentence> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<Sentence> sentences = new ArrayList<>();
        for (List<Line> paragraph : paragraphs(lines(text))) {
            sentences.addAll(splitParagraph(paragraph));
        }
        return List.copyOf(sentences);
    }

    private record Line(String text, int start, int end) {
    }

    private record Paragraph(String text, int[] source) {
    }

    private List<Line> lines(String text) {
        List<Line> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                result.add(new Line(text.substring(start, i), start, i));
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        result.add(new Line(text.substring(start), start, text.length()));
        return result;
    }

    private List<List<Line>> paragraphs(List<Line> lines) {
        List<List<Line>> result = new ArrayList<>();
        List<Line> current = new ArrayList<>();
        for (Line line : lines) {
            if (line.text().isBlank()) {
                flush(current, result);
                continue;
            }
            if (current.isEmpty()) {
                current.add(line);
            } else if (shouldJoin(current.get(current.size() - 1), line)) {
                current.add(line);
            } else {
                flush(current, result);
                current.add(line);
            }
        }
        flush(current, result);
        return result;
    }

    private void flush(List<Line> current, List<List<Line>> result) {
        if (!current.isEmpty()) {
            result.add(List.copyOf(current));
            current.clear();
        }
    }

    private boolean shouldJoin(Line current, Line next) {
        String c = current.text().strip();
        String n = next.text().strip();
        if (c.isEmpty() || n.isEmpty()) {
            return false;
        }
        if (endsSentence(c) || isBullet(n)) {
            return false;
        }
        return !isHeader(c) && !isHeader(n);
    }

    private boolean endsSentence(String line) {
        char last = line.charAt(line.length() - 1);
        return last == '.' || last == '!' || last == '?';
    }

    private boolean isBullet(String line) {
        return !line.isEmpty() && BULLETS.indexOf(line.charAt(0)) >= 0;
    }

    private boolean isHeader(String line) {
        if (line.length() > 40) {
            return false;
        }
        boolean hasLetter = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (Character.isLowerCase(c)) {
                    return false;
                }
            }
        }
        return hasLetter;
    }

    private Paragraph paragraph(List<Line> lines) {
        StringBuilder value = new StringBuilder();
        List<Integer> source = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (i > 0) {
                value.append(' ');
                source.add(Math.max(0, line.start() - 1));
            }
            for (int k = 0; k < line.text().length(); k++) {
                value.append(line.text().charAt(k));
                source.add(line.start() + k);
            }
        }
        int[] map = source.stream().mapToInt(Integer::intValue).toArray();
        return new Paragraph(value.toString(), map);
    }

    private List<Sentence> splitParagraph(List<Line> lines) {
        Paragraph paragraph = paragraph(lines);
        String text = paragraph.text();
        int[] source = paragraph.source();
        List<Sentence> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isSeparator(text, i)) {
                addSentence(result, text, source, start, i + 1);
                start = i + 1;
            }
        }
        addSentence(result, text, source, start, text.length());
        return result;
    }

    private boolean isSeparator(String text, int i) {
        char c = text.charAt(i);
        if (c != '.' && c != '!' && c != '?' && c != ';') {
            return false;
        }
        if (c != '.') {
            return true;
        }
        if (i > 0 && i + 1 < text.length()
                && Character.isDigit(text.charAt(i - 1))
                && Character.isDigit(text.charAt(i + 1))) {
            return false;
        }
        return !isDottedName(text, i);
    }

    /**
     * Nomes como "Node.js" e "ASP.NET" tem ponto no meio. Um ponto seguido de
     * um trecho curto, todo em minusculas ou todo em maiusculas, e tratado como
     * parte do nome; "fim.Tambem" continua sendo separador.
     */
    private boolean isDottedName(String text, int dot) {
        int end = dot + 1;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (Character.isWhitespace(c) || c == '.' || c == '!' || c == '?' || c == ';') {
                break;
            }
            end++;
        }
        int length = end - dot - 1;
        if (length == 0 || length > 4) {
            return false;
        }
        boolean lower = true;
        boolean upper = true;
        for (int i = dot + 1; i < end; i++) {
            char c = text.charAt(i);
            if (!Character.isLetter(c)) {
                return false;
            }
            lower &= Character.isLowerCase(c);
            upper &= Character.isUpperCase(c);
        }
        return lower || upper;
    }

    private void addSentence(List<Sentence> result, String text, int[] source, int from, int to) {
        int start = from;
        int end = to;
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        if (from == 0) {
            while (start < end && isBullet(text.charAt(start))) {
                start++;
            }
            while (start < end && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return;
        }
        String sentence = text.substring(start, end).replaceAll("\\s+", " ").strip();
        if (!sentence.isEmpty()) {
            result.add(new Sentence(sentence, source[start], source[end - 1] + 1));
        }
    }

    private boolean isBullet(char c) {
        return BULLETS.indexOf(c) >= 0;
    }
}
