package com.resumematch.matcher;

import static com.resumematch.matcher.RequirementPriority.PREFERRED;
import static com.resumematch.matcher.RequirementPriority.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void termosEntreParentesesViramUmConjunto() {
        assertThat(extractor.requirements("Noções de redes (TCP/IP, DNS, DHCP)."))
                .containsExactly(
                        new JobRequirement("Redes de Computadores", REQUIRED),
                        new JobRequirement("TCP/IP + DNS + DHCP", REQUIRED,
                                List.of("TCP/IP", "DNS", "DHCP")));
        assertThat(extractor.requirements("Sistemas operacionais (Windows/Linux)"))
                .containsExactly(new JobRequirement("Windows + Linux", REQUIRED,
                        List.of("Windows", "Linux")));
    }

    @Test
    void parentesesComAlternativasDaMesmaFamiliaContinuaAlternativa() {
        assertThat(extractor.requirements("Noções de nuvem (AWS, Azure ou GCP)."))
                .containsExactly(new JobRequirement("AWS / Azure / GCP", REQUIRED));
    }

    @Test
    void parentesesComExemplosViraAlternativa() {
        assertThat(extractor.extract("Ferramentas de RPA (ex.: UiPath, Blue Prism ou similares)."))
                .containsExactly("RPA", "UiPath / Blue Prism");
        assertThat(extractor.extract("Linguagens ou scripts (Python, VBA, SQL, etc.)."))
                .containsExactly("Python / VBA / SQL");
    }

    @Test
    void parentesesComUmTermoSoNaoMudaNada() {
        assertThat(extractor.extract("Automacoes com RPA (Robotic Process Automation) e Docker (containers)."))
                .containsExactly("RPA", "Docker");
    }

    @Test
    void termoDoConjuntoNaoEntraDeNovoSozinho() {
        assertThat(extractor.extract("Conhecimento em Linux. Sistemas operacionais (Windows/Linux)."))
                .containsExactly("Windows + Linux");
    }

    @Test
    void leParentesesDeVagaCopiadaDoLinkedin() throws IOException {
        String job = Files.readString(Path.of("src/test/resources/vagas/vaga1_estagioTI.txt"));

        assertThat(extractor.requirements(job)).containsExactly(
                new JobRequirement("Help Desk", REQUIRED),
                new JobRequirement("RPA", REQUIRED),
                new JobRequirement("Windows + Linux", REQUIRED, List.of("Windows", "Linux")),
                new JobRequirement("Redes de Computadores", REQUIRED),
                new JobRequirement("Hardware", REQUIRED),
                new JobRequirement("Microsoft Office", REQUIRED),
                new JobRequirement("UiPath / Automation Anywhere / Blue Prism", REQUIRED),
                new JobRequirement("Python / VBA / SQL", PREFERRED));
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
    void listaTrechosDeRequisitoQueNaoForamAvaliados() {
        JobExtraction job = extractor.read("""
                Requisitos
                Java.
                Boa comunicação.
                Inglês técnico para leitura de documentação.
                Cursando ensino superior em Redes de Computadores.
                """);

        assertThat(job.requirements()).containsExactly(new JobRequirement("Java", REQUIRED));
        assertThat(job.unevaluated()).containsExactly(
                new UnevaluatedItem("Boa comunicação.", JobSection.REQUIREMENTS,
                        UnevaluatedItem.Reason.NO_KNOWN_TERM),
                new UnevaluatedItem("Inglês técnico para leitura de documentação.",
                        JobSection.REQUIREMENTS, UnevaluatedItem.Reason.LANGUAGE),
                new UnevaluatedItem("Cursando ensino superior em Redes de Computadores.",
                        JobSection.REQUIREMENTS, UnevaluatedItem.Reason.EDUCATION));
        assertThat(job.unknownTerms()).isEmpty();
    }

    @Test
    void cursoNaFormacaoNaoViraRequisitoTecnico() {
        assertThat(extractor.extract("Cursando Ciência da Computação ou Redes de Computadores.")).isEmpty();
        assertThat(extractor.extract("Estar cursando Sistemas de Informação. Conhecimento em Redes de Computadores."))
                .containsExactly("Redes de Computadores");
    }

    @Test
    void naoListaApresentacaoNemBeneficios() {
        JobExtraction job = extractor.read("""
                Sobre a vaga
                Somos uma Empresa Legal que usa Java.
                Benefícios
                Vale Refeição.
                """);

        assertThat(job.requirements()).containsExactly(new JobRequirement("Java", REQUIRED));
        assertThat(job.unevaluated()).isEmpty();
        assertThat(job.unknownTerms()).isEmpty();
    }

    @Test
    void listaNomesQueODicionarioNaoConhece() {
        JobExtraction job = extractor.read("""
                Diferenciais
                Ferramentas de chamados (Movidesk, Octadesk) ou scripts simples.
                Apoiar contas (Active Directory, Google Workspace / Microsoft 365) nível N1/N2.
                """);

        assertThat(job.unknownTerms()).containsExactly("Movidesk", "Octadesk", "N1", "N2");
        assertThat(job.unevaluated()).extracting(UnevaluatedItem::reason)
                .containsExactly(UnevaluatedItem.Reason.NO_KNOWN_TERM);
    }

    @Test
    void reconheceFerramentasDeChamadoEScriptsDaVagaDeInfra() {
        JobExtraction job = extractor.read("""
                Será um diferencial:
                Conhecimento básico em ferramentas de chamados (Jira, Zendesk, GLPI) ou scripts simples (PowerShell/Bash);
                """);

        assertThat(job.requirements()).containsExactly(
                new JobRequirement("Jira / Zendesk / GLPI", PREFERRED),
                new JobRequirement("PowerShell / Bash", PREFERRED));
        assertThat(job.unknownTerms()).isEmpty();
        assertThat(job.unevaluated()).isEmpty();
    }

    @Test
    void parentesesDaMesmaFamiliaSemOuViraAlternativa() {
        assertThat(extractor.extract("Nuvem (AWS, Azure, GCP).")).containsExactly("AWS / Azure / GCP");
        assertThat(extractor.extract("Protocolos (TCP/IP, DNS, DHCP).")).containsExactly("TCP/IP + DNS + DHCP");
    }

    @Test
    void tiraMarcadorDeListaDoTrechoNaoAvaliado() throws IOException {
        String job = Files.readString(
                Path.of("src/test/resources/vagas-sinteticas/vaga3_estagio_infra_cloud.txt"));

        assertThat(extractor.read(job).unevaluated()).contains(
                new UnevaluatedItem("Estar cursando ensino superior em TI.", JobSection.REQUIREMENTS,
                        UnevaluatedItem.Reason.EDUCATION),
                new UnevaluatedItem("Documentar procedimentos técnicos.", JobSection.RESPONSIBILITIES,
                        UnevaluatedItem.Reason.NO_KNOWN_TERM));
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
