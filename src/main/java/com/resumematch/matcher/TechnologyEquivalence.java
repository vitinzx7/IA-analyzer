package com.resumematch.matcher;

import java.util.Map;
import java.util.Set;

/**
 * Relacoes em que um termo mais especifico satisfaz um requisito mais generico.
 *
 * A relacao e de mao unica: Spring Boot satisfaz Spring, mas Spring nao
 * satisfaz uma vaga que peca Spring Boot.
 */
public final class TechnologyEquivalence {

    private static final Map<String, Set<String>> ACCEPTED = Map.of(
            "Spring Framework", Set.of("Spring Boot"),
            "Microsoft Office", Set.of("Office 365"),
            "SQL", Set.of("PostgreSQL", "MySQL", "SQL Server", "Oracle", "H2"),
            "Azure", Set.of("Azure Container Apps"),
            "Docker", Set.of("Docker Compose"));

    private TechnologyEquivalence() {
    }

    public static boolean satisfies(String requirement, String technology) {
        return requirement.equals(technology)
                || ACCEPTED.getOrDefault(requirement, Set.of()).contains(technology);
    }
}
