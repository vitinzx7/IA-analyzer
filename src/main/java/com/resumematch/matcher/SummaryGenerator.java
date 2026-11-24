package com.resumematch.matcher;

import java.util.List;

/**
 * Gera o resumo textual entregue junto com o score.
 */
public interface SummaryGenerator {

    String generate(List<RequirementMatch> requirements, double score);

    /** Versao que conhece a leitura da vaga, para citar o que nao foi avaliado. */
    default String generate(List<RequirementMatch> requirements, double score, JobExtraction job) {
        return generate(requirements, score);
    }
}
