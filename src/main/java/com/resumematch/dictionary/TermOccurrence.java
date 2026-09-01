package com.resumematch.dictionary;

/**
 * Uma ocorrencia de tecnologia encontrada num texto.
 *
 * @param technology nome canonico, ja resolvido a partir do alias
 * @param matchedText trecho exatamente como aparece no texto original
 * @param start indice inicial no texto original, inclusivo
 * @param end indice final no texto original, exclusivo
 */
public record TermOccurrence(String technology, String matchedText, int start, int end) {
}
