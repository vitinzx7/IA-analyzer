package com.resumematch.matcher;

/**
 * Uma frase do curriculo e o intervalo que ela ocupa no texto original.
 */
public record Sentence(String text, int start, int end) {

    public Sentence {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("texto da frase nao pode ser vazio");
        }
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("intervalo da frase invalido");
        }
    }
}
