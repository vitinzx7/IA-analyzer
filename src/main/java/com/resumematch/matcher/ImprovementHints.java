package com.resumematch.matcher;

import java.util.List;
import java.util.Map;

/**
 * Indicios de mao unica: um termo do curriculo que sugere outro requisito.
 *
 * Servem so para a simulacao de melhora e nunca entram na cobertura real. Java
 * indica Back-end, mas Back-end nao indica Java: saber que a pessoa faz
 * back-end nao diz qual linguagem ela usa. Por isso a tabela e consultada pelo
 * requisito ausente, e nao pelo termo do curriculo.
 */
final class ImprovementHints {

    private static final Map<String, List<String>> INDICATED_BY = Map.of(
            "Git", List.of("GitHub"),
            "Back-end", List.of("Java", "Spring Boot", "API REST"),
            "Front-end", List.of("React", "HTML", "CSS", "JavaScript"));

    private ImprovementHints() {
    }

    /** Termos do curriculo que indicam o requisito, na ordem de preferencia. */
    static List<String> indicatorsOf(String requirement) {
        return INDICATED_BY.getOrDefault(requirement, List.of());
    }
}
