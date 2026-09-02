package com.resumematch.matcher;

public record RequirementMatch(String name, MatchStatus status, String evidence) {
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
    }
}
