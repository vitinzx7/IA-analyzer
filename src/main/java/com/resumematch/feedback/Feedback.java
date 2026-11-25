package com.resumematch.feedback;

import java.util.regex.Pattern;

import com.resumematch.analysis.AnalysisException;
import com.resumematch.matcher.MatchStatus;

/**
 * Marcacao de uma pessoa sobre uma decisao do matcher.
 *
 * Guarda so o nome do requisito e os dois status: nenhum trecho do curriculo
 * vai para o registro.
 *
 * @param analysisId identificador da analise, gerado pela tela, para que duas
 *        marcacoes do mesmo requisito na mesma analise contem uma vez so
 * @param requirement nome do requisito como veio no resultado
 * @param programStatus status que o programa deu
 * @param correctStatus status que a pessoa diz ser o certo
 */
public record Feedback(String analysisId, String requirement, MatchStatus programStatus,
        MatchStatus correctStatus) {

    static final int MAX_REQUIREMENT = 200;
    private static final Pattern ANALYSIS_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    public Feedback {
        if (analysisId == null || !ANALYSIS_ID.matcher(analysisId).matches()) {
            throw new AnalysisException("Identificador da analise invalido.");
        }
        if (requirement == null || requirement.isBlank() || requirement.length() > MAX_REQUIREMENT
                || requirement.chars().anyMatch(Character::isISOControl)) {
            throw new AnalysisException("Nome do requisito invalido.");
        }
        if (programStatus == null || correctStatus == null) {
            throw new AnalysisException("Informe o status do programa e o status correto.");
        }
        requirement = requirement.strip();
    }

    public boolean correct() {
        return programStatus == correctStatus;
    }
}
