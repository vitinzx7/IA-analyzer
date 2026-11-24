package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MatcherModelsTest {

    @Test
    void defineOsTresStatusPossiveis() {
        assertThat(MatchStatus.values())
                .containsExactly(MatchStatus.MATCH, MatchStatus.PARTIAL, MatchStatus.MISSING);
    }

    @Test
    void representaRequisitoComEvidencia() {
        RequirementMatch match = new RequirementMatch(
                "Java", MatchStatus.MATCH, "Desenvolvi uma API com Java.");

        assertThat(match.name()).isEqualTo("Java");
        assertThat(match.status()).isEqualTo(MatchStatus.MATCH);
        assertThat(match.evidence()).isEqualTo("Desenvolvi uma API com Java.");
    }

    @Test
    void requisitoSemPrioridadeInformadaEhObrigatorio() {
        assertThat(new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi.").priority())
                .isEqualTo(RequirementPriority.REQUIRED);
        assertThat(new RequirementMatch("Java", MatchStatus.MATCH, "Desenvolvi.", null).priority())
                .isEqualTo(RequirementPriority.REQUIRED);
        assertThat(new JobRequirement("Java", null).priority())
                .isEqualTo(RequirementPriority.REQUIRED);
    }

    @Test
    void permiteAusenciaDeEvidenciaParaRequisitoFaltante() {
        RequirementMatch match = new RequirementMatch("AWS", MatchStatus.MISSING, null);

        assertThat(match.evidence()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"MATCH", "PARTIAL"})
    void exigeEvidenciaQuandoRequisitoFoiEncontrado(MatchStatus status) {
        assertThatThrownBy(() -> new RequirementMatch("Java", status, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exige evidencia");
    }

    @Test
    void rejeitaEvidenciaQuandoRequisitoEstaFaltando() {
        assertThatThrownBy(() -> new RequirementMatch(
                "AWS", MatchStatus.MISSING, "Experiencia com AWS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode ter evidencia");
    }

    @Test
    void resultadoNaoMudaSeAListaOriginalForAlterada() {
        RequirementMatch match = new RequirementMatch(
                "Docker", MatchStatus.PARTIAL, "Tecnologias: Docker.");
        List<RequirementMatch> requirements = new ArrayList<>(List.of(match));

        AnalysisResult result = new AnalysisResult(50.0, requirements, "Resumo local");
        requirements.clear();

        assertThat(result.score()).isEqualTo(50.0);
        assertThat(result.requirements()).containsExactly(match);
        assertThat(result.summary()).isEqualTo("Resumo local");
    }
}
