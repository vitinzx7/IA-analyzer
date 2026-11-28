package com.resumematch.log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifica a vaga para a memoria de vagas.
 *
 * Com link de vaga do LinkedIn no texto, a chave e o numero da vaga. Sem link,
 * a chave e o comeco do SHA-256 do texto em minusculas e com os espacos
 * juntados, para que a mesma vaga colada de novo caia na mesma chave.
 *
 * Cargo, empresa e local so sao lidos quando o link esta sozinho numa linha e
 * antes dele ha duas ou tres linhas curtas: cargo, empresa e, se houver,
 * local. Em qualquer outro formato ficam vazios, porque adivinhar o cargo pela
 * primeira linha de uma vaga colada a mao erraria com frequencia.
 */
record JobHeader(String key, String title, String company, String location, String link) {

    private static final Pattern LINKEDIN_JOB = Pattern.compile(
            "linkedin\\.com/jobs/view/(?:[\\w%-]*-)?(\\d+)|[?&]currentJobId=(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_HEADER_LINE = 150;
    private static final int KEY_HEX_CHARS = 16;

    static JobHeader read(String jobText) {
        String text = jobText == null ? "" : jobText;
        Matcher found = LINKEDIN_JOB.matcher(text);
        if (!found.find()) {
            return new JobHeader("texto:" + digest(text), "", "", "", "");
        }
        String id = found.group(1) != null ? found.group(1) : found.group(2);
        String link = "https://www.linkedin.com/jobs/view/" + id + "/";
        List<String> header = headerBefore(text, found.start());
        if (header.size() < 2 || header.size() > 3) {
            return new JobHeader("linkedin:" + id, "", "", "", link);
        }
        return new JobHeader("linkedin:" + id, header.get(0), header.get(1),
                header.size() == 3 ? header.get(2) : "", link);
    }

    /** Linhas nao vazias antes da linha do link, ou nenhuma se o link nao esta sozinho. */
    private static List<String> headerBefore(String text, int linkPosition) {
        int lineStart = text.lastIndexOf('\n', linkPosition) + 1;
        int lineEnd = text.indexOf('\n', linkPosition);
        String linkLine = text.substring(lineStart, lineEnd < 0 ? text.length() : lineEnd).strip();
        if (linkLine.chars().anyMatch(Character::isWhitespace)) {
            return List.of();
        }
        List<String> lines = text.substring(0, lineStart).lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
        if (lines.stream().anyMatch(line -> line.length() > MAX_HEADER_LINE)) {
            return List.of();
        }
        return lines;
    }

    private static String digest(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, KEY_HEX_CHARS);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
