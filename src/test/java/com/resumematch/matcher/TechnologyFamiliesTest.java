package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TechnologyFamiliesTest {

    @Test
    void reconheceTermosDaMesmaFamilia() {
        assertThat(TechnologyFamilies.sameFamily(List.of("Azure", "AWS", "GCP"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("PostgreSQL", "MySQL"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("GitHub", "GitLab"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("Maven", "Gradle"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("Jira", "Zendesk", "GLPI"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("PowerShell", "Bash"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("Google Workspace", "Office 365"))).isTrue();
        assertThat(TechnologyFamilies.sameFamily(List.of("VMware", "Hyper-V"))).isTrue();
    }

    @Test
    void recusaFamiliasMisturadasOuTermoSozinho() {
        assertThat(TechnologyFamilies.sameFamily(List.of("Git", "GitHub"))).isFalse();
        assertThat(TechnologyFamilies.sameFamily(List.of("Windows", "Linux"))).isFalse();
        assertThat(TechnologyFamilies.sameFamily(List.of("Azure", "PostgreSQL"))).isFalse();
        assertThat(TechnologyFamilies.sameFamily(List.of("Azure"))).isFalse();
        assertThat(TechnologyFamilies.sameFamily(List.of())).isFalse();
    }
}
