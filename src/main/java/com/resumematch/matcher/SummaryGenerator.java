package com.resumematch.matcher;

import java.util.List;

/**
 * Gera o resumo textual entregue junto com o score.
 */
public interface SummaryGenerator {

    String generate(List<RequirementMatch> requirements, double score);
}
