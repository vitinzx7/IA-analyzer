package com.resumematch.analysis;

import com.resumematch.matcher.PreviousAnalysis;

/**
 * Consulta se a vaga ja foi analisada antes.
 *
 * Falha de leitura nao pode derrubar a analise: quem implementa devolve null.
 */
public interface JobHistory {

    JobHistory NONE = jobText -> null;

    /** @return analises anteriores da mesma vaga, ou null se nao houver */
    PreviousAnalysis previous(String jobText);
}
