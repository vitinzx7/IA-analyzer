package com.resumematch.matcher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.dictionary.TermOccurrence;

/**
 * Extrai os requisitos reconheciveis de uma descricao de vaga.
 *
 * O dicionario e a fonte da verdade: termos fora dele nao entram no calculo.
 */
public final class JobRequirementExtractor {

    private final TechnologyDictionary dictionary;

    public JobRequirementExtractor(TechnologyDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<String> extract(String jobText) {
        if (jobText == null || jobText.isBlank()) {
            return List.of();
        }
        Set<String> requirements = new LinkedHashSet<>();
        for (TermOccurrence occurrence : dictionary.findIn(jobText)) {
            requirements.add(occurrence.technology());
        }
        return List.copyOf(requirements);
    }
}
