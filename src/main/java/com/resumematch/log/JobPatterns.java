package com.resumematch.log;

import java.util.List;

/**
 * O que as vagas da memoria pedem, contando cada vaga uma vez so (a analise
 * mais recente de cada chave).
 *
 * @param enabled false quando a memoria de vagas esta desligada
 * @param jobs vagas diferentes na memoria
 * @param analyses linhas lidas, contando as repeticoes
 * @param requirements requisitos do dicionario, do mais pedido ao menos pedido
 * @param unknownTerms nomes que o dicionario nao conhece, do mais visto ao menos visto
 */
public record JobPatterns(boolean enabled, int jobs, int analyses, List<Requirement> requirements,
        List<Term> unknownTerms) {

    public JobPatterns {
        requirements = List.copyOf(requirements);
        unknownTerms = List.copyOf(unknownTerms);
    }

    static JobPatterns disabled() {
        return new JobPatterns(false, 0, 0, List.of(), List.of());
    }

    /**
     * @param jobs vagas que pedem o requisito
     * @param required vagas em que ele e obrigatorio
     * @param preferred vagas em que ele e so diferencial
     */
    public record Requirement(String name, int jobs, int required, int preferred) {
    }

    public record Term(String name, int jobs) {
    }
}
