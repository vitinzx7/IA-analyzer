package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ActionVerbsTest {

    @Test
    void reconheceOsVerbosDaLista() {
        List<String> verbs = List.of(
                "desenvolvi", "implementei", "criei", "publiquei", "realizei",
                "utilizei", "configurei", "apoiei", "mantive", "escrevi");

        for (String verb : verbs) {
            assertThat(ActionVerbs.contains("Eu " + verb + " uma solucao."))
                    .as(verb)
                    .isTrue();
        }
    }

    @Test
    void ignoraVerboDePosse() {
        assertThat(ActionVerbs.contains("Possuo conhecimentos em Java")).isFalse();
    }

    @Test
    void ignoraSubstantivoDerivado() {
        assertThat(ActionVerbs.contains("Desenvolvimento de APIs")).isFalse();
    }

    @Test
    void naoDependeDeCaixaNemDeAcento() {
        assertThat(ActionVerbs.contains("UTILIZEI Docker")).isTrue();
        assertThat(ActionVerbs.contains("Configurei o ambiente")).isTrue();
    }

    @Test
    void ignoraTextoNuloOuVazio() {
        assertThat(ActionVerbs.contains(null)).isFalse();
        assertThat(ActionVerbs.contains("")).isFalse();
        assertThat(ActionVerbs.contains("   ")).isFalse();
    }
}
