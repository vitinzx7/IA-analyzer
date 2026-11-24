package com.resumematch.matcher;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formato do requisito que aceita mais de uma tecnologia: "Azure / AWS / GCP".
 *
 * O requisito continua sendo uma String para que score, resumo, CSV e tela nao
 * mudem. Esta classe e o unico lugar que conhece o separador; nenhum nome do
 * dicionario contem " / " com espacos (CI/CD e TCP/IP nao tem).
 */
public final class RequirementAlternatives {

    static final String SEPARATOR = " / ";

    private RequirementAlternatives() {
    }

    public static String join(Collection<String> technologies) {
        return String.join(SEPARATOR, technologies);
    }

    public static List<String> options(String requirement) {
        return List.of(requirement.split(Pattern.quote(SEPARATOR)));
    }
}
