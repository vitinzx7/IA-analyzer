package com.resumematch.matcher;

/**
 * Decisao do matcher para um requisito da vaga.
 *
 * @param priority peso do requisito no score; quem nao informa recebe
 *        REQUIRED, que vale 1
 */
public record RequirementMatch(String name, MatchStatus status, String evidence,
        RequirementPriority priority) {

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
    }

    public RequirementMatch(String name, MatchStatus status, String evidence) {
        this(name, status, evidence, RequirementPriority.REQUIRED);
    }
}
