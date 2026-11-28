package com.resumematch.analysis;

import com.resumematch.matcher.AnalysisResult;

/**
 * Registro auxiliar de uma analise concluida.
 *
 * Falha de registro nao pode derrubar a analise principal.
 */
public interface AnalysisLog {

    AnalysisLog NONE = (result, jobText) -> {
    };

    /**
     * @param jobText descricao da vaga como chegou na analise
     */
    void log(AnalysisResult result, String jobText);
}
