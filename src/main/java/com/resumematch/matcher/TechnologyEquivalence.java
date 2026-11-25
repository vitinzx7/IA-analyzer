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

    private static final Map<String, Set<String>> ACCEPTED = Map.ofEntries(
            Map.entry("Spring Framework", Set.of("Spring Boot")),
            Map.entry("Microsoft Office", Set.of("Office 365")),
            Map.entry("SQL", Set.of("PostgreSQL", "MySQL", "SQL Server", "Oracle", "H2")),
            Map.entry("Azure", Set.of("Azure Container Apps")),
            Map.entry("Docker", Set.of("Docker Compose")),
            Map.entry("Linux", Set.of("Kali Linux", "Ubuntu", "Debian", "Red Hat")),
            Map.entry("Windows", Set.of("Windows Server")),
            Map.entry("Shell Script", Set.of("Bash")));

    private TechnologyEquivalence() {
    }

    public static boolean satisfies(String requirement, String technology) {
        return requirement.equals(technology)
                || ACCEPTED.getOrDefault(requirement, Set.of()).contains(technology);
    }
}
