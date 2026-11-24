package com.resumematch.matcher;

import java.util.List;
import java.util.Objects;

/**
 * Um requisito lido da vaga, com o peso que ele tem no score.
 *
 * @param name nome canonico, varias opcoes no formato de
 *        {@link RequirementAlternatives}, ou as partes de um conjunto ligadas
 *        por {@link #COMPONENT_SEPARATOR}
 * @param priority obrigatorio ou diferencial
 * @param components partes de um conjunto, como TCP/IP, DNS e DHCP em
 *        "noções de redes (TCP/IP, DNS, DHCP)"; vazia nos demais requisitos
 */
public record JobRequirement(String name, RequirementPriority priority, List<String> components) {

    public static final String COMPONENT_SEPARATOR = " + ";

    public JobRequirement {
        Objects.requireNonNull(name, "name");
        priority = priority == null ? RequirementPriority.REQUIRED : priority;
        components = components == null ? List.of() : List.copyOf(components);
    }

    public JobRequirement(String name, RequirementPriority priority) {
        this(name, priority, List.of());
    }

    public static JobRequirement composite(List<String> components, RequirementPriority priority) {
        return new JobRequirement(String.join(COMPONENT_SEPARATOR, components), priority, components);
    }

    public boolean isComposite() {
        return !components.isEmpty();
    }
}
