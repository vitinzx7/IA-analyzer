package com.resumematch.matcher;

/**
 * Secao da descricao da vaga de onde um trecho veio.
 *
 * GENERAL cobre o texto antes do primeiro titulo reconhecido e secoes como
 * "Sobre a vaga". IGNORED cobre beneficios, local, horario e remuneracao: o
 * que esta ali nao e requisito.
 */
public enum JobSection {
    GENERAL,
    REQUIREMENTS,
    RESPONSIBILITIES,
    PREFERRED,
    IGNORED
}
