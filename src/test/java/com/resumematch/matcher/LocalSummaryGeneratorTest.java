package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class LocalSummaryGeneratorTest {

    private final LocalSummaryGenerator generator = new LocalSummaryGenerator();

    private RequirementMatch match(String name, MatchStatus status) {
        String evidence = status == MatchStatus.MISSING ? null : "evidencia";
        return new RequirementMatch(name, status, evidence);
    }

    @Test
    void avisaQueDiferencialValeMetadeSoQuandoHaDiferencial() {
        String comDiferencial = generator.generate(List.of(
                match("Java", MatchStatus.MATCH),
                new RequirementMatch("AWS", MatchStatus.MISSING, null, RequirementPriority.PREFERRED)), 66.7);
        String semDiferencial = generator.generate(List.of(match("Java", MatchStatus.MATCH)), 100.0);

        assertThat(comDiferencial).contains("Diferenciais valem metade");
        assertThat(semDiferencial).doesNotContain("Diferenciais");
    }

    @Test
    void contaOsTrechosQueNaoForamAvaliados() {
        JobExtraction job = new JobExtraction(List.of(), List.of(
                new UnevaluatedItem("Boa comunicação.", JobSection.REQUIREMENTS,
                        UnevaluatedItem.Reason.NO_KNOWN_TERM),
                new UnevaluatedItem("Inglês técnico.", JobSection.REQUIREMENTS,
                        UnevaluatedItem.Reason.LANGUAGE)), List.of());

        assertThat(generator.generate(List.of(match("Java", MatchStatus.MATCH)), 100.0, job))
                .contains("2 trecho(s) de requisito da vaga nao foram avaliados")
                .endsWith("nao e nota do candidato nem probabilidade de contratacao.");
        assertThat(generator.generate(List.of(), 0.0, job))
                .contains("Nenhum requisito reconhecido")
                .contains("2 trecho(s)");
    }

    @Test
    void resumeAsContagensDoResultado() {
        String summary = generator.generate(List.of(
                match("Java", MatchStatus.MATCH),
                match("Docker", MatchStatus.PARTIAL),
                match("AWS", MatchStatus.MISSING)), 50.0);

        assertThat(summary)
                .contains("Cobertura tecnica 50/100")
                .contains("1 MATCH")
                .contains("1 PARTIAL")
                .contains("1 MISSING");
    }

    @Test
    void sugestaoParaAusenteEhCondicional() {
        String summary = generator.generate(List.of(
                match("AWS", MatchStatus.MISSING)), 0.0);

        assertThat(summary).contains("Se voce realmente tem experiencia");
        assertThat(summary.toLowerCase()).doesNotContain("adicione");
    }

    @Test
    void avisaQueRequisitosForaDoDicionarioNaoEntramNoCalculo() {
        String summary = generator.generate(List.of(
                match("Java", MatchStatus.MATCH)), 100.0);

        assertThat(summary).contains("fora do dicionario nao entram no calculo");
        assertThat(summary).contains("nao e nota do candidato nem probabilidade de contratacao");
    }

    @Test
    void quandoTudoCasaNaoSugereNada() {
        String summary = generator.generate(List.of(
                match("Java", MatchStatus.MATCH),
                match("Docker", MatchStatus.MATCH)), 100.0);

        assertThat(summary).contains("Todos os requisitos reconhecidos");
        assertThat(summary).doesNotContain("Nao encontrei evidencia");
        assertThat(summary).doesNotContain("Aparecem so em listas");
    }

    @Test
    void listaVaziaExplicaQueNadaFoiReconhecido() {
        assertThat(generator.generate(List.of(), 0.0))
                .contains("Nenhum requisito reconhecido")
                .contains("nao foi calculado");
    }

    @Test
    void limitaAQuantidadeDeNomesCitados() {
        List<RequirementMatch> missing = List.of(
                match("A", MatchStatus.MISSING),
                match("B", MatchStatus.MISSING),
                match("C", MatchStatus.MISSING),
                match("D", MatchStatus.MISSING),
                match("E", MatchStatus.MISSING),
                match("F", MatchStatus.MISSING));

        assertThat(generator.generate(missing, 0.0)).contains("e mais 1");
    }
}
