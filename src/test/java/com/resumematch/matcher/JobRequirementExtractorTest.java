package com.resumematch.matcher;

import static com.resumematch.matcher.RequirementPriority.PREFERRED;
import static com.resumematch.matcher.RequirementPriority.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.resumematch.dictionary.TechnologyDictionary;

class JobRequirementExtractorTest {

    private final JobRequirementExtractor extractor =
            new JobRequirementExtractor(new TechnologyDictionary());

    @Test
    void extraiTecnologiasNaOrdemDaVaga() {
        assertThat(extractor.extract("Requisitos: Java, Spring Boot e Docker. Desejavel AWS."))
                .containsExactly("Java", "Spring Boot", "Docker", "AWS");
    }

    @Test
    void resolveAliasesParaONomeCanonico() {
        assertThat(extractor.extract("Postgres, js e k8s"))
                .containsExactly("PostgreSQL", "JavaScript", "Kubernetes");
    }

    @Test
    void naoRepeteTecnologia() {
        assertThat(extractor.extract("Java e mais Java no dia a dia")).containsExactly("Java");
    }

    @Test
    void ignoraTextoVazio() {
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract("   ")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }

    @Test
    void agrupaAlternativasDaMesmaFamilia() {
        assertThat(extractor.extract("Cloud: Azure, AWS ou GCP.")).containsExactly("Azure / AWS / GCP");
        assertThat(extractor.extract("PostgreSQL ou MySQL")).containsExactly("PostgreSQL / MySQL");
        assertThat(extractor.extract("Azure ou AWS ou GCP")).containsExactly("Azure / AWS / GCP");
        assertThat(extractor.extract("Repositorio no GitHub/GitLab")).containsExactly("GitHub / GitLab");
        assertThat(extractor.extract("Azure e/ou AWS")).containsExactly("Azure / AWS");
        assertThat(extractor.extract("Azure e AWS")).containsExactly("Azure", "AWS");
    }

    @Test
    void naoAgrupaListaDeFamiliasDiferentes() {
        assertThat(extractor.extract("Requisitos: Java, Spring Boot, Docker, Git ou GitHub."))
                .containsExactly("Java", "Spring Boot", "Docker", "Git", "GitHub");
    }

    @Test
    void barraEntreWindowsELinuxPedeOsDois() {
        assertThat(extractor.extract("Suporte em ambiente Windows/Linux."))
                .containsExactly("Windows", "Linux");
    }

    @Test
    void agrupaSoOFinalDaListaQueEhDaMesmaFamilia() {
        assertThat(extractor.extract("Stack: Java, Docker, Azure, AWS ou GCP."))
                .containsExactly("Java", "Docker", "Azure / AWS / GCP");
    }

    @Test
    void tecnologiaDoGrupoNaoEntraDeNovoSozinha() {
        assertThat(extractor.extract("Azure, AWS ou GCP. Experiencia com Azure."))
                .containsExactly("Azure / AWS / GCP");
        assertThat(extractor.extract("Experiencia com Azure. Cloud: Azure, AWS ou GCP."))
                .containsExactly("Azure / AWS / GCP");
    }

    @Test
    void termoComBarraNoNomeNaoViraAlternativa() {
        assertThat(extractor.extract("Conhecimento em TCP/IP e CI/CD"))
                .containsExactly("TCP/IP", "CI/CD");
    }

    @Test
    void requisitoDaSecaoDiferenciaisViraDiferencial() {
        assertThat(extractor.requirements("Requisitos\nJava e Docker.\nDiferenciais\nAWS."))
                .containsExactly(
                        new JobRequirement("Java", REQUIRED),
                        new JobRequirement("Docker", REQUIRED),
                        new JobRequirement("AWS", PREFERRED));
    }

    @Test
    void trechoComDesejavelViraDiferencialSemMudarORestoDaLinha() {
        assertThat(extractor.requirements(
                "Requisitos: Java. Desejavel Terraform. Experiencia com Docker."))
                .containsExactly(
                        new JobRequirement("Java", REQUIRED),
                        new JobRequirement("Terraform", PREFERRED),
                        new JobRequirement("Docker", REQUIRED));
    }

    @Test
    void ignoraBeneficiosLocalEHorario() {
        String job = """
                Requisitos: Java.
                Benefícios oferecidos:
                Curso de AWS pago pela empresa.
                Local de atuação: sala com Docker e Kubernetes
                """;
        assertThat(extractor.extract(job)).containsExactly("Java");
    }

    @Test
    void tituloComDoisPontosValeAteOProximoTitulo() {
        assertThat(extractor.requirements("Diferenciais: Kubernetes\nTerraform\nRequisitos:\nJava"))
                .containsExactly(
                        new JobRequirement("Kubernetes", PREFERRED),
                        new JobRequirement("Terraform", PREFERRED),
                        new JobRequirement("Java", REQUIRED));
    }

    @Test
    void linhaQueComecaComPalavraDeTituloNaoMudaASecao() {
        assertThat(extractor.requirements(
                "Diferenciais\nConhecimentos em Java\nHabilidades de comunicacao\nKubernetes"))
                .containsExactly(
                        new JobRequirement("Java", PREFERRED),
                        new JobRequirement("Kubernetes", PREFERRED));
    }

    @Test
    void obrigatorioNoTrechoVenceASecao() {
        assertThat(extractor.requirements("Diferenciais:\nAWS.\nImprescindível experiência com Java."))
                .containsExactly(
                        new JobRequirement("AWS", PREFERRED),
                        new JobRequirement("Java", REQUIRED));
    }

    @Test
    void requisitoEmDuasSecoesFicaComAMaiorPrioridade() {
        assertThat(extractor.requirements("Diferenciais: Java.\nRequisitos: Java e Docker."))
                .containsExactly(
                        new JobRequirement("Java", REQUIRED),
                        new JobRequirement("Docker", REQUIRED));
    }

    @Test
    void leSecoesDeVagaCopiadaDoLinkedin() throws IOException {
        String job = Files.readString(Path.of("src/test/resources/vagas/vaga3_suport.txt"));

        assertThat(extractor.requirements(job)).containsExactly(
                new JobRequirement("ITIL", PREFERRED),
                new JobRequirement("Windows", PREFERRED),
                new JobRequirement("Office 365", PREFERRED));
    }

    @Test
    void leSecoesComMarcadorDeLista() throws IOException {
        String job = Files.readString(
                Path.of("src/test/resources/vagas-sinteticas/vaga3_estagio_infra_cloud.txt"));

        assertThat(extractor.requirements(job)).containsExactly(
                new JobRequirement("Linux", REQUIRED),
                new JobRequirement("Redes de Computadores", REQUIRED),
                new JobRequirement("Docker", REQUIRED),
                new JobRequirement("Git", REQUIRED),
                new JobRequirement("GitHub", REQUIRED),
                new JobRequirement("Azure / AWS", PREFERRED),
                new JobRequirement("Kubernetes", PREFERRED),
                new JobRequirement("Terraform", PREFERRED));
    }
}
