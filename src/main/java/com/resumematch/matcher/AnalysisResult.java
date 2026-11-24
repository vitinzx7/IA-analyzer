package com.resumematch.matcher;

import java.util.List;

/**
 * Resultado da analise.
 *
 * @param score nota de 0 a 100, ou null quando nenhum requisito da vaga foi
 *        reconhecido e, portanto, nao ha nota a calcular
 */
public record AnalysisResult(Double score, List<RequirementMatch> requirements, String summary) {

    public AnalysisResult {
        requirements = List.copyOf(requirements);
    }
}
