package com.resumematch.matcher;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Familias de tecnologias que se substituem numa vaga.
 *
 * "Azure, AWS ou GCP" so vira um requisito de alternativas quando todos os
 * termos sao da mesma familia. As familias sao pequenas de proposito e crescem
 * pelos erros vistos no CSV. Windows e Linux ficam de fora: em vaga de suporte,
 * "Windows/Linux" costuma pedir os dois.
 */
public final class TechnologyFamilies {

    private static final List<Set<String>> FAMILIES = List.of(
            Set.of("Azure", "AWS", "GCP"),
            Set.of("PostgreSQL", "MySQL", "SQL Server", "Oracle"),
            Set.of("GitHub", "GitLab"),
            Set.of("Maven", "Gradle"),
            Set.of("Jira", "Zendesk", "GLPI", "ServiceNow"),
            Set.of("PowerShell", "Bash", "Shell Script"),
            Set.of("Google Workspace", "Office 365"),
            Set.of("VMware", "Hyper-V", "VirtualBox", "Proxmox"),
            Set.of("Zabbix", "Nagios", "Prometheus", "Grafana"),
            Set.of("Splunk", "Wazuh"));

    private TechnologyFamilies() {
    }

    public static boolean sameFamily(Collection<String> technologies) {
        if (technologies == null || technologies.size() < 2) {
            return false;
        }
        return FAMILIES.stream().anyMatch(family -> family.containsAll(technologies));
    }
}
