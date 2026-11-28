package com.resumematch.matcher;

/**
 * Analises anteriores da mesma vaga, lidas da memoria de vagas.
 *
 * @param count quantas vezes a vaga ja foi analisada antes desta
 * @param lastAt data e hora da ultima, no formato "yyyy-MM-dd HH:mm:ss"
 * @param lastCoverage cobertura da ultima; null quando ela nao teve calculo
 */
public record PreviousAnalysis(int count, String lastAt, Double lastCoverage) {
}
