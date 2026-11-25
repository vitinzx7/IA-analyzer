package com.resumematch.matcher;

import java.util.List;

/**
 * Resultado da simulacao de melhora. Nao muda a cobertura real.
 *
 * @param suggestions mudancas que dao pontos, da que da mais para a que da menos
 * @param scoreWithAll cobertura com todas as sugestoes aplicadas juntas; null
 *        quando nao ha sugestao
 * @param gaps requisitos sem evidencia e sem indicio, que ficam fora da conta
 */
public record ImprovementSimulation(List<ImprovementSuggestion> suggestions, Double scoreWithAll,
        List<RealGap> gaps) {

    public static final ImprovementSimulation EMPTY = new ImprovementSimulation(List.of(), null, List.of());

    public ImprovementSimulation {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
    }
}
