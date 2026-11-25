package com.resumematch.matcher;

/**
 * Uma mudanca no curriculo que aumentaria a cobertura, se for verdade.
 *
 * @param indicator termo do curriculo que indica o requisito; null no tipo
 *        LIST_ONLY
 * @param evidence trecho do curriculo que justifica a sugestao
 * @param scoreWithChange cobertura com so esta mudanca aplicada
 * @param points scoreWithChange menos a cobertura real
 */
public record ImprovementSuggestion(String requirement, SuggestionType type, String indicator,
        String evidence, RequirementPriority priority, double scoreWithChange, double points) {
}
