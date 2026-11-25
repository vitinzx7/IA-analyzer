package com.resumematch.matcher;

import java.util.List;

import com.resumematch.pdf.HiddenTextReport;

/**
 * Resultado da analise.
 *
 * @param score nota de 0 a 100, ou null quando nenhum requisito da vaga foi
 *        reconhecido e, portanto, nao ha nota a calcular
 * @param unevaluated trechos de requisito da vaga que ficaram fora do calculo
 * @param unknownTerms nomes da vaga que parecem tecnologia mas o dicionario
 *        nao conhece
 * @param pdfCheck texto escondido achado no PDF; null quando o curriculo veio
 *        como texto colado
 * @param improvement simulacao de melhora; nunca altera o score
 */
public record AnalysisResult(Double score, List<RequirementMatch> requirements, String summary,
        List<UnevaluatedItem> unevaluated, List<String> unknownTerms, HiddenTextReport pdfCheck,
        ImprovementSimulation improvement) {

    public AnalysisResult {
        requirements = List.copyOf(requirements);
        unevaluated = unevaluated == null ? List.of() : List.copyOf(unevaluated);
        unknownTerms = unknownTerms == null ? List.of() : List.copyOf(unknownTerms);
        improvement = improvement == null ? ImprovementSimulation.EMPTY : improvement;
    }

    public AnalysisResult(Double score, List<RequirementMatch> requirements, String summary,
            List<UnevaluatedItem> unevaluated, List<String> unknownTerms, HiddenTextReport pdfCheck) {
        this(score, requirements, summary, unevaluated, unknownTerms, pdfCheck, null);
    }

    public AnalysisResult(Double score, List<RequirementMatch> requirements, String summary,
            List<UnevaluatedItem> unevaluated, List<String> unknownTerms) {
        this(score, requirements, summary, unevaluated, unknownTerms, null);
    }

    public AnalysisResult(Double score, List<RequirementMatch> requirements, String summary) {
        this(score, requirements, summary, List.of(), List.of());
    }
}
