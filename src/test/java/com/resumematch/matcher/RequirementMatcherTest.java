package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.resumematch.dictionary.TechnologyDictionary;

class RequirementMatcherTest {

    private final SentenceSplitter splitter = new SentenceSplitter();
    private final RequirementMatcher matcher =
            new RequirementMatcher(new TechnologyDictionary());

    private List<RequirementMatch> match(String resume, String... requirements) {
        return matcher.match(List.of(requirements), resume, splitter.split(resume));
    }

    @Test
    void classificaMatchPartialEMissing() {
        List<RequirementMatch> result = match(
                "Desenvolvi uma API em Java com Spring Boot.", "Java", "Spring Boot", "AWS");

        assertThat(result).extracting(RequirementMatch::name)
                .containsExactly("Java", "Spring Boot", "AWS");
        assertThat(result).extracting(RequirementMatch::status)
                .containsExactly(MatchStatus.MATCH, MatchStatus.MATCH, MatchStatus.MISSING);
        assertThat(result.get(2).evidence()).isNull();
    }

    @Test
    void evidenciaEhAFraseQueSustentaADecisao() {
        List<RequirementMatch> result = match("Desenvolvi uma API em Java.", "Java");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().evidence()).isEqualTo("Desenvolvi uma API em Java.");
    }

    @Test
    void listaSoltaFicaPartial() {
        List<RequirementMatch> result = match("Tecnologias: Java, Docker, Git", "Docker");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.PARTIAL);
        assertThat(result.getFirst().evidence()).isEqualTo("Tecnologias: Java, Docker, Git");
    }

    @Test
    void matchVencePartialQuandoApareceNosDoisContextos() {
        List<RequirementMatch> result = match(
                "Tecnologias: Docker.\nDesenvolvi o deploy com Docker.", "Docker");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().evidence()).isEqualTo("Desenvolvi o deploy com Docker.");
    }

    @Test
    void naoDeixaUmVerboDeOutraFrasePromoverORequisito() {
        List<RequirementMatch> result = match(
                "Tecnologias: Java.\nDesenvolvi o deploy com Docker.", "Java");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.PARTIAL);
        assertThat(result.getFirst().evidence()).isEqualTo("Tecnologias: Java.");
    }

    @Test
    void resolveAliasDeVagaParaTermoDoCurriculo() {
        List<RequirementMatch> result = match("Desenvolvi consultas com Postgres.", "PostgreSQL");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().name()).isEqualTo("PostgreSQL");
    }

    @Test
    void javascriptNaoViraJava() {
        List<RequirementMatch> result = match(
                "Desenvolvi o front em JavaScript.", "Java", "JavaScript");

        assertThat(result).extracting(RequirementMatch::status)
                .containsExactly(MatchStatus.MISSING, MatchStatus.MATCH);
    }

    @Test
    void springBootSatisfazRequisitoDeSpring() {
        List<RequirementMatch> result = match(
                "Desenvolvi uma API com Spring Boot.", "Spring Framework");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().evidence()).contains("Spring Boot");
    }

    @Test
    void springNaoSatisfazRequisitoDeSpringBoot() {
        List<RequirementMatch> result = match(
                "Desenvolvi uma API com Spring Framework.", "Spring Boot");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MISSING);
    }

    @Test
    void postgresSatisfazRequisitoDeSql() {
        List<RequirementMatch> result = match(
                "Desenvolvi consultas com PostgreSQL.", "SQL");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
    }

    @Test
    void azureContainerAppsSatisfazRequisitoDeAzure() {
        List<RequirementMatch> result = match(
                "Desenvolvi e publiquei a aplicacao na Azure Container Apps.", "Azure");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().evidence()).contains("Azure Container Apps");
    }

    @Test
    void dockerComposeSatisfazRequisitoDeDocker() {
        List<RequirementMatch> result = match(
                "Configurei o ambiente com Docker Compose.", "Docker");

        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().evidence()).contains("Docker Compose");
    }

    @Test
    void termoGenericoNaoSatisfazRequisitoEspecifico() {
        assertThat(match("Desenvolvi na Azure.", "Azure Container Apps").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
        assertThat(match("Utilizei Docker.", "Docker Compose").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
    }

    @Test
    void semRequisitosDevolveListaVazia() {
        assertThat(matcher.match(List.of(), "Desenvolvi uma API.", List.of())).isEmpty();
        assertThat(matcher.match(null, "Desenvolvi uma API.", List.of())).isEmpty();
    }

    @Test
    void grupoDeAlternativasAceitaQualquerOpcao() {
        List<RequirementMatch> result = match(
                "Desenvolvi e publiquei no Azure Container Apps.", "Azure / AWS / GCP");

        assertThat(result.getFirst().name()).isEqualTo("Azure / AWS / GCP");
        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.getFirst().evidence()).isEqualTo("Desenvolvi e publiquei no Azure Container Apps.");
    }

    @Test
    void grupoDeAlternativasSegueAMesmaRegraDeEvidencia() {
        assertThat(match("Cloud: GCP.", "Azure / AWS / GCP").getFirst().status())
                .isEqualTo(MatchStatus.PARTIAL);
        assertThat(match("Desenvolvi uma API em Java.", "Azure / AWS / GCP").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
    }
}
