package com.resumematch.matcher;

import java.util.List;

/**
 * Decisao do matcher para um requisito da vaga.
 *
 * @param priority peso do requisito no score; quem nao informa recebe
 *        REQUIRED, que vale 1
 * @param components decisao de cada parte, quando o requisito e um conjunto;
 *        vazia nos demais
 * @param evidenceSection secao do curriculo de onde a evidencia veio; null
 *        quando nao ha evidencia
 */
public record RequirementMatch(String name, MatchStatus status, String evidence,
        RequirementPriority priority, List<ComponentMatch> components,
        ResumeSection evidenceSection) {

    public RequirementMatch {
        if (status == null) {
            throw new IllegalArgumentException("status nao pode ser nulo");
        }

        if (status == MatchStatus.MISSING && evidence != null) {
            throw new IllegalArgumentException("MISSING nao pode ter evidencia");
        }

        if (status != MatchStatus.MISSING && evidence == null) {
            throw new IllegalArgumentException(status + " exige evidencia");
        }

        priority = priority == null ? RequirementPriority.REQUIRED : priority;
        components = components == null ? List.of() : List.copyOf(components);
        evidenceSection = status == MatchStatus.MISSING ? null : evidenceSection;
    }

    public RequirementMatch(String name, MatchStatus status, String evidence,
            RequirementPriority priority, List<ComponentMatch> components) {
        this(name, status, evidence, priority, components, null);
    }

    public RequirementMatch(String name, MatchStatus status, String evidence,
            RequirementPriority priority) {
        this(name, status, evidence, priority, List.of());
    }

    public RequirementMatch(String name, MatchStatus status, String evidence) {
        this(name, status, evidence, RequirementPriority.REQUIRED);
    }
}
