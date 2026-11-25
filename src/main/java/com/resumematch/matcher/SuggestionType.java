package com.resumematch.matcher;

/**
 * Tipo de sugestao da simulacao de melhora.
 */
public enum SuggestionType {
    /** A: o requisito aparece, mas so em lista ou sem frase de acao. */
    LIST_ONLY,
    /** B: o requisito falta, mas outro termo do curriculo o indica. */
    INDICATED
}
