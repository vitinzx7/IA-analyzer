package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
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

    private RequirementMatch matchComposite(String resume, String... components) {
        JobRequirement composite = JobRequirement.composite(List.of(components), RequirementPriority.REQUIRED);
        return matcher.matchRequirements(List.of(composite), resume, splitter.split(resume)).getFirst();
    }

    @Test
    void conjuntoComTodasAsPartesComAcaoEhMatch() {
        RequirementMatch result = matchComposite(
                "Configurei DNS, DHCP e TCP/IP no laboratorio.", "TCP/IP", "DNS", "DHCP");

        assertThat(result.name()).isEqualTo("TCP/IP + DNS + DHCP");
        assertThat(result.status()).isEqualTo(MatchStatus.MATCH);
        assertThat(result.components()).extracting(ComponentMatch::status)
                .containsOnly(MatchStatus.MATCH);
    }

    @Test
    void conjuntoComParteDasPartesEhPartialComEvidenciaDaParteComAcao() {
        RequirementMatch result = matchComposite(
                "Tecnologias: TCP/IP.\nConfigurei o DNS do laboratorio.", "TCP/IP", "DNS", "DHCP");

        assertThat(result.status()).isEqualTo(MatchStatus.PARTIAL);
        assertThat(result.evidence()).isEqualTo("Configurei o DNS do laboratorio.");
        assertThat(result.components()).extracting(ComponentMatch::status)
                .containsExactly(MatchStatus.PARTIAL, MatchStatus.MATCH, MatchStatus.MISSING);
    }

    @Test
    void conjuntoSemNenhumaParteEhMissing() {
        RequirementMatch result = matchComposite("Desenvolvi uma API em Java.", "TCP/IP", "DNS");

        assertThat(result.status()).isEqualTo(MatchStatus.MISSING);
        assertThat(result.evidence()).isNull();
        assertThat(result.components()).hasSize(2);
    }

    @Test
    void requisitoSimplesNaoTemPartes() {
        assertThat(match("Desenvolvi uma API em Java.", "Java").getFirst().components()).isEmpty();
    }

    @Test
    void distribuicaoLinuxEWindowsServerSatisfazemOGenerico() {
        assertThat(match("Configurei o Kali Linux no laboratorio.", "Linux").getFirst().status())
                .isEqualTo(MatchStatus.MATCH);
        assertThat(match("Configurei o Windows Server 2022 no laboratorio.", "Windows").getFirst().status())
                .isEqualTo(MatchStatus.MATCH);
        assertThat(match("Configurei o Linux no laboratorio.", "Kali Linux").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
    }

    @Test
    void mantemAPrioridadeDoRequisito() {
        String resume = "Desenvolvi uma API em Java.";
        List<RequirementMatch> result = matcher.matchRequirements(List.of(
                new JobRequirement("Java", RequirementPriority.REQUIRED),
                new JobRequirement("AWS", RequirementPriority.PREFERRED)), resume, splitter.split(resume));

        assertThat(result).extracting(RequirementMatch::priority)
                .containsExactly(RequirementPriority.REQUIRED, RequirementPriority.PREFERRED);
        assertThat(result).extracting(RequirementMatch::status)
                .containsExactly(MatchStatus.MATCH, MatchStatus.MISSING);
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
    void termoNaLinhaSeguinteAUmaAnotacaoContinuaMatch() {
        assertThat(match("• Implementei endpoints com @RestController e\nSpring Boot no backend.",
                "Spring Boot").getFirst().status()).isEqualTo(MatchStatus.MATCH);
        assertThat(match("• Desenvolvi a API em Java | camada de servico e\nPostgreSQL como banco.",
                "PostgreSQL").getFirst().status()).isEqualTo(MatchStatus.MATCH);
    }

    @Test
    void evidenciaSaiSemEmailETelefone() {
        RequirementMatch result = match(
                "Victor | Desenvolvedor Java | victor@email.com | (61) 99999-9999", "Java").getFirst();

        assertThat(result.evidence())
                .isEqualTo("Victor | Desenvolvedor Java | [email] | [telefone]")
                .doesNotContain("victor@email.com", "99999");
    }

    @Test
    void termoNegadoLogoAntesNaoEhEvidencia() {
        assertThat(match("Desenvolvi sistemas sem Docker.", "Docker").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
        assertThat(match("Desenvolvi sistemas sem o Docker.", "Docker").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
    }

    @Test
    void oracaoComVerboNegadoNaoEhEvidencia() {
        assertThat(match("Nunca utilizei AWS.", "AWS").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
        assertThat(match("Não configurei Kubernetes.", "Kubernetes").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
    }

    @Test
    void negacaoValeSoNaPropriaOracao() {
        List<RequirementMatch> result = match(
                "Nunca utilizei AWS, mas configurei Azure.", "AWS", "Azure");

        assertThat(result).extracting(RequirementMatch::status)
                .containsExactly(MatchStatus.MISSING, MatchStatus.MATCH);
        assertThat(match("Sem experiência prévia, desenvolvi uma API em Java.", "Java")
                .getFirst().status()).isEqualTo(MatchStatus.MATCH);
    }

    @Test
    void semQueNaoNegaOTermoNaoAtrapalha() {
        assertThat(match("Configurei pipelines sem downtime com Jenkins.", "Jenkins")
                .getFirst().status()).isEqualTo(MatchStatus.MATCH);

        List<RequirementMatch> result = match("Desenvolvi APIs em Java sem Spring.", "Java", "Spring Framework");
        assertThat(result).extracting(RequirementMatch::status)
                .containsExactly(MatchStatus.MATCH, MatchStatus.MISSING);
    }

    @Test
    void mencaoNegadaNaoApagaOutraMencaoValida() {
        RequirementMatch result = match(
                "Tecnologias: Docker.\nDesenvolvi sistemas sem Docker.", "Docker").getFirst();

        assertThat(result.status()).isEqualTo(MatchStatus.PARTIAL);
        assertThat(result.evidence()).isEqualTo("Tecnologias: Docker.");
    }

    @Test
    void textoComMuitasRepeticoesNaoFicaQuadratico() {
        String repeated = "Java, ".repeat(30_000);
        String manySentences = "Tecnologias: Java. ".repeat(10_000);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertThat(match(repeated, "Java", "Docker")).extracting(RequirementMatch::status)
                    .containsExactly(MatchStatus.PARTIAL, MatchStatus.MISSING);
            assertThat(match(manySentences, "Java").getFirst().status()).isEqualTo(MatchStatus.PARTIAL);
        });
    }

    @Test
    void termoDentroDeEmailNaoEhEvidencia() {
        assertThat(match("Contato: dev.java@empresa.com", "Java").getFirst().status())
                .isEqualTo(MatchStatus.MISSING);
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
