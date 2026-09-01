package com.resumematch.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TechnologyDictionaryTest {

    private final TechnologyDictionary dictionary = new TechnologyDictionary();

    private List<String> technologiesIn(String text) {
        return dictionary.findIn(text).stream().map(TermOccurrence::technology).toList();
    }

    @Test
    void naoEncontraJavaDentroDeJavaScript() {
        assertThat(technologiesIn("Experiencia com JavaScript e React"))
                .containsExactly("JavaScript", "React")
                .doesNotContain("Java");
    }

    @Test
    void encontraJavaQuandoEstaSozinho() {
        assertThat(technologiesIn("Desenvolvi APIs com Java 21")).contains("Java");
    }

    @Test
    void resolveAliasParaONomeCanonico() {
        assertThat(technologiesIn("Banco Postgres, front em js e deploy em k8s"))
                .containsExactlyInAnyOrder("PostgreSQL", "JavaScript", "Kubernetes");
    }

    @Test
    void casaOTermoMaisLongoPrimeiro() {
        assertThat(technologiesIn("Usei Docker Compose no projeto"))
                .containsExactly("Docker Compose")
                .doesNotContain("Docker");
    }

    @Test
    void naoEncontraCDentroDeCSustenido() {
        assertThat(technologiesIn("Projetos em C# e .NET"))
                .containsExactlyInAnyOrder("C#", ".NET");
    }

    @Test
    void encontraTermoAcentuadoDoTextoOriginal() {
        assertThat(technologiesIn("Conhecimento em redes de computadores"))
                .contains("Redes de Computadores");
    }

    @Test
    void guardaOTrechoOriginalComAcentoECaixa() {
        List<TermOccurrence> found = dictionary.findIn("Trabalhei com Docker e PostgreSQL");

        assertThat(found).extracting(TermOccurrence::matchedText)
                .containsExactly("Docker", "PostgreSQL");
    }

    @Test
    void indicesApontamParaOTextoOriginal() {
        String text = "Experiência em manutenção com Docker";

        TermOccurrence docker = dictionary.findIn(text).getFirst();

        assertThat(text.substring(docker.start(), docker.end())).isEqualTo("Docker");
    }

    @Test
    void ignoraTextoVazio() {
        assertThat(dictionary.findIn("")).isEmpty();
        assertThat(dictionary.findIn(null)).isEmpty();
    }

    @Test
    void naoRepeteOMesmoTrechoEmDoisTermos() {
        assertThat(technologiesIn("Azure Container Apps")).containsExactly("Azure Container Apps");
    }

    @Test
    void pacoteOffice365NaoViraMicrosoftOffice() {
        assertThat(technologiesIn("conhecimento do pacote Office 365"))
                .containsExactly("Office 365");
    }

    @Test
    void pacoteOfficeSozinhoContinuaSendoMicrosoftOffice() {
        assertThat(technologiesIn("Pacote Office / ferramentas corporativas"))
                .containsExactly("Microsoft Office");
    }
}
