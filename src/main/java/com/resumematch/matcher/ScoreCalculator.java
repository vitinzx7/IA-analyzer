package com.resumematch.matcher;

import java.util.List;

/**
 * Calcula o score deterministico a partir dos status dos requisitos.
 *
 * MATCH vale 1, PARTIAL vale 0,5 e MISSING vale 0. O score e a razao entre os
 * pontos obtidos e os pontos possiveis, em uma escala de 0 a 100.
 */
public final class ScoreCalculator {

    public double calculate(List<RequirementMatch> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return 0.0;
        }
        double points = 0.0;
        for (RequirementMatch requirement : requirements) {
            points += switch (requirement.status()) {
                case MATCH -> 1.0;
                case PARTIAL -> 0.5;
                case MISSING -> 0.0;
            };
        }
        double score = points / requirements.size() * 100.0;
        return Math.round(score * 10.0) / 10.0;
    }
}
