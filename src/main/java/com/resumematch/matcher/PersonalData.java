package com.resumematch.matcher;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconhece e mascara dado de contato no texto do curriculo.
 *
 * So padroes reais contam: um "@" solto, como em "@RestController", ou um "|"
 * nao fazem a linha virar contato.
 */
public final class PersonalData {

    // O e-mail so comeca no inicio de uma palavra e a parte antes do "@" nao
    // volta atras (++). Sem isso, um texto longo sem "@" custa tempo quadratico.
    private static final Pattern EMAIL = Pattern.compile(
            "(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]++@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    // Exige DDD: sem ele, "2026-2028" teria o mesmo formato de um telefone de
    // oito digitos. Os limites de digito impedem casar no meio de um numero.
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\w+])(?:\\+?55[\\s.-]?)?\\(?\\d{2}\\)?[\\s.-]?(?:9[\\s.-]?)?\\d{4}[\\s.-]?\\d{4}(?!\\d)");

    private static final Pattern LINK = Pattern.compile(
            "https?://|www\\.|github\\.com|linkedin\\.com", Pattern.CASE_INSENSITIVE);

    private PersonalData() {
    }

    public static boolean isContactLine(String line) {
        return line != null
                && (EMAIL.matcher(line).find()
                        || PHONE.matcher(line).find()
                        || LINK.matcher(line).find());
    }

    /**
     * Marca as posicoes do texto que pertencem a um e-mail. Um termo como
     * "java" em "dev.java@empresa.com" nao e evidencia de nada.
     */
    public static boolean[] emailPositions(String text) {
        boolean[] inside = new boolean[text == null ? 0 : text.length()];
        if (text == null) {
            return inside;
        }
        Matcher email = EMAIL.matcher(text);
        while (email.find()) {
            Arrays.fill(inside, email.start(), email.end(), true);
        }
        return inside;
    }

    public static String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = EMAIL.matcher(text).replaceAll("[email]");
        return PHONE.matcher(masked).replaceAll("[telefone]");
    }
}
