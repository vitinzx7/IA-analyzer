package com.resumematch.matcher;

/**
 * Trecho de uma secao de requisitos que nao entrou no calculo.
 *
 * @param text o trecho como aparece na vaga, sem marcador de lista
 * @param section secao de onde o trecho veio
 * @param reason por que o trecho ficou de fora
 * @param resumeEvidence para formacao e idioma, o trecho do curriculo que
 *        parece responder ao pedido; null quando nao ha ou quando nao se aplica
 */
public record UnevaluatedItem(String text, JobSection section, Reason reason, String resumeEvidence) {

    public enum Reason {
        /** Formacao academica: o programa ainda nao compara curso. */
        EDUCATION,
        /** Idioma: o programa ainda nao compara idioma. */
        LANGUAGE,
        /** Nenhum termo do dicionario no trecho. */
        NO_KNOWN_TERM
    }

    public UnevaluatedItem(String text, JobSection section, Reason reason) {
        this(text, section, reason, null);
    }

    UnevaluatedItem withResumeEvidence(String evidence) {
        return new UnevaluatedItem(text, section, reason, evidence);
    }
}
