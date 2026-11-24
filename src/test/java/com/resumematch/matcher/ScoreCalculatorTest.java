package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    private RequirementMatch match(String name, MatchStatus status) {
        String evidence = status == MatchStatus.MISSING ? null : "evidencia";
        return new RequirementMatch(name, status, evidence);
    }

    @Test
    void calculaFormulaDaRegra() {
        List<RequirementMatch> requirements = List.of(
                match("Java", MatchStatus.MATCH),
                match("Spring Boot", MatchStatus.MATCH),
                match("Docker", MatchStatus.MATCH),
                match("AWS", MatchStatus.PARTIAL),
                match("Kubernetes", MatchStatus.MISSING));

        assertThat(calculator.calculate(requirements)).isEqualTo(70.0);
    }

    @Test
    void todosMatchDevolveCem() {
        assertThat(calculator.calculate(List.of(
                match("Java", MatchStatus.MATCH),
                match("Docker", MatchStatus.MATCH)))).isEqualTo(100.0);
    }

    @Test
    void todosMissingDevolveZero() {
        assertThat(calculator.calculate(List.of(
                match("Java", MatchStatus.MISSING),
                match("Docker", MatchStatus.MISSING)))).isEqualTo(0.0);
    }

    @Test
    void arredondaParaUmaCasaDecimal() {
        assertThat(calculator.calculate(List.of(
                match("Java", MatchStatus.MATCH),
                match("Docker", MatchStatus.MATCH),
                match("AWS", MatchStatus.MISSING)))).isEqualTo(66.7);
    }

    @Test
    void diferencialPesaMetade() {
        assertThat(calculator.calculate(List.of(
                new RequirementMatch("Java", MatchStatus.MATCH, "evidencia", RequirementPriority.REQUIRED),
                new RequirementMatch("AWS", MatchStatus.MISSING, null, RequirementPriority.PREFERRED))))
                .isEqualTo(66.7);
    }

    @Test
    void diferencialEncontradoNaoCompensaObrigatorioAusente() {
        assertThat(calculator.calculate(List.of(
                new RequirementMatch("Java", MatchStatus.MISSING, null, RequirementPriority.REQUIRED),
                new RequirementMatch("AWS", MatchStatus.MATCH, "evidencia", RequirementPriority.PREFERRED))))
                .isEqualTo(33.3);
    }

    @Test
    void listaVaziaOuNulaDevolveZero() {
        assertThat(calculator.calculate(List.of())).isEqualTo(0.0);
        assertThat(calculator.calculate(null)).isEqualTo(0.0);
    }
}
