package com.resumematch.matcher;

import java.util.List;

/**
 * Requisito que nenhuma reescrita resolve: falta evidencia e nenhum indicio.
 *
 * @param missingParts partes sem evidencia, quando o requisito e um conjunto;
 *        vazia nos demais
 */
public record RealGap(String requirement, RequirementPriority priority, List<String> missingParts) {

    public RealGap {
        missingParts = missingParts == null ? List.of() : List.copyOf(missingParts);
    }
}
