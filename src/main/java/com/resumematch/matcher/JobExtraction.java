package com.resumematch.matcher;

import java.util.List;

/**
 * O que a leitura da vaga produziu.
 *
 * @param requirements requisitos reconhecidos pelo dicionario
 * @param unevaluated trechos de requisito que ficaram fora do calculo
 * @param unknownTerms nomes que parecem tecnologia mas o dicionario nao conhece
 */
public record JobExtraction(List<JobRequirement> requirements, List<UnevaluatedItem> unevaluated,
        List<String> unknownTerms) {

    public static final JobExtraction EMPTY = new JobExtraction(List.of(), List.of(), List.of());

    public JobExtraction {
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        unevaluated = unevaluated == null ? List.of() : List.copyOf(unevaluated);
        unknownTerms = unknownTerms == null ? List.of() : List.copyOf(unknownTerms);
    }
}
