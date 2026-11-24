package com.resumematch.matcher;

/**
 * Trecho de uma secao de requisitos que nao entrou no calculo.
 *
 * @param text o trecho como aparece na vaga, sem marcador de lista
 * @param section secao de onde o trecho veio
 * @param reason por que o trecho ficou de fora
 */
public record UnevaluatedItem(String text, JobSection section, Reason reason) {

    public enum Reason {
        /** Formacao academica: o programa ainda nao compara curso. */
        EDUCATION,
        /** Idioma: o programa ainda nao compara idioma. */
        LANGUAGE,
        /** Nenhum termo do dicionario no trecho. */
        NO_KNOWN_TERM
    }
}
