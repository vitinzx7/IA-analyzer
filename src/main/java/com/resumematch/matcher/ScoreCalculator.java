package com.resumematch.matcher;

import java.util.List;

/**
 * Calcula o score deterministico a partir dos status dos requisitos.
 *
 * MATCH vale 1, PARTIAL vale 0,5 e MISSING vale 0. Cada requisito pesa o que
 * a sua prioridade diz: obrigatorio pesa 1 e diferencial pesa 0,5. O score e a
 * razao entre os pontos obtidos e os pontos possiveis, em uma escala de 0 a 100.
 */
public final class ScoreCalculator {

    public double calculate(List<RequirementMatch> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return 0.0;
        }
        double points = 0.0;
        double possible = 0.0;
        for (RequirementMatch requirement : requirements) {
            double weight = requirement.priority().weight();
            possible += weight;
            points += weight * switch (requirement.status()) {
                case MATCH -> 1.0;
                case PARTIAL -> 0.5;
                case MISSING -> 0.0;
            };
        }
        double score = points / possible * 100.0;
        return Math.round(score * 10.0) / 10.0;
    }
}
