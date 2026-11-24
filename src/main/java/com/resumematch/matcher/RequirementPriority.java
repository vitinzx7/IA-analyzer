package com.resumematch.matcher;

/**
 * Peso do requisito no score.
 *
 * Os dois valores sao um ponto de partida, nao uma medida: precisam ser
 * calibrados com decisoes marcadas como certas ou erradas.
 */
public enum RequirementPriority {
    REQUIRED(1.0),
    PREFERRED(0.5);

    private final double weight;

    RequirementPriority(double weight) {
        this.weight = weight;
    }

    public double weight() {
        return weight;
    }

    static RequirementPriority highest(RequirementPriority a, RequirementPriority b) {
        return a == REQUIRED || b == REQUIRED ? REQUIRED : PREFERRED;
    }
}
