package com.resumematch.matcher;

import java.util.Objects;

/**
 * Um requisito lido da vaga, com o peso que ele tem no score.
 *
 * @param name nome canonico, ou varias opcoes no formato de
 *        {@link RequirementAlternatives}
 * @param priority obrigatorio ou diferencial
 */
public record JobRequirement(String name, RequirementPriority priority) {

    public JobRequirement {
        Objects.requireNonNull(name, "name");
        priority = priority == null ? RequirementPriority.REQUIRED : priority;
    }
}
