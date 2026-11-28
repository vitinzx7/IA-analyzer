package com.resumematch.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JobHeaderTest {

    private static final String COPIED = """
            Desenvolvedor Java Junior
            Empresa Exemplo
            Brasilia, Distrito Federal, Brasil · Hibrido · Tempo integral
            https://www.linkedin.com/jobs/view/4123456789/

            Sobre a vaga

            Requisitos: Java, SQL e Git.
            """;

    @Test
    void leCargoEmpresaLocalELinkDoCabecalho() {
        JobHeader job = JobHeader.read(COPIED);

        assertThat(job.key()).isEqualTo("linkedin:4123456789");
        assertThat(job.title()).isEqualTo("Desenvolvedor Java Junior");
        assertThat(job.company()).isEqualTo("Empresa Exemplo");
        assertThat(job.location()).isEqualTo("Brasilia, Distrito Federal, Brasil · Hibrido · Tempo integral");
        assertThat(job.link()).isEqualTo("https://www.linkedin.com/jobs/view/4123456789/");
    }

    @Test
    void cabecalhoSemLocalTemCargoEEmpresa() {
        JobHeader job = JobHeader.read("Analista de Suporte\nEmpresa X\nhttps://www.linkedin.com/jobs/view/42/\n\nRequisitos: Linux.");

        assertThat(job.title()).isEqualTo("Analista de Suporte");
        assertThat(job.company()).isEqualTo("Empresa X");
        assertThat(job.location()).isEmpty();
    }

    @Test
    void umaLinhaSoAntesDoLinkNaoViraCargo() {
        JobHeader job = JobHeader.read("Brasilia, DF\nhttps://www.linkedin.com/jobs/view/42/\n\nRequisitos: Linux.");

        assertThat(job.key()).isEqualTo("linkedin:42");
        assertThat(job.title()).isEmpty();
        assertThat(job.company()).isEmpty();
        assertThat(job.location()).isEmpty();
    }

    @Test
    void linkNoMeioDoTextoDaAChaveMasNaoCabecalho() {
        JobHeader job = JobHeader.read("Requisitos: Java.\nCandidate-se em https://www.linkedin.com/jobs/view/77/ ate sexta.");

        assertThat(job.key()).isEqualTo("linkedin:77");
        assertThat(job.link()).isEqualTo("https://www.linkedin.com/jobs/view/77/");
        assertThat(job.title()).isEmpty();
    }

    @Test
    void maisDeTresLinhasAntesDoLinkNaoSaoCabecalho() {
        JobHeader job = JobHeader.read("a\nb\nc\nd\nhttps://www.linkedin.com/jobs/view/42/");

        assertThat(job.title()).isEmpty();
        assertThat(job.company()).isEmpty();
    }

    @Test
    void linhaLongaAntesDoLinkNaoECabecalho() {
        JobHeader job = JobHeader.read("Cargo\n" + "x".repeat(200) + "\nhttps://www.linkedin.com/jobs/view/42/");

        assertThat(job.title()).isEmpty();
    }

    @Test
    void aceitaLinkComNomeDaVagaEBuscaComCurrentJobId() {
        assertThat(JobHeader.read("https://br.linkedin.com/jobs/view/desenvolvedor-java-at-empresa-4555/").key())
                .isEqualTo("linkedin:4555");
        assertThat(JobHeader.read("https://www.linkedin.com/jobs/search-results/?currentJobId=4666&keywords=java").key())
                .isEqualTo("linkedin:4666");
    }

    @Test
    void semLinkAChaveVemDoTextoEIgnoraEspacosEMaiusculas() {
        JobHeader first = JobHeader.read("Requisitos:  Java e SQL.\n");
        JobHeader again = JobHeader.read("requisitos: java e sql.");
        JobHeader other = JobHeader.read("Requisitos: Python.");

        assertThat(first.key()).startsWith("texto:").hasSize("texto:".length() + 16);
        assertThat(again.key()).isEqualTo(first.key());
        assertThat(other.key()).isNotEqualTo(first.key());
        assertThat(first.title()).isEmpty();
        assertThat(first.link()).isEmpty();
    }

    @Test
    void textoNuloNaoQuebra() {
        assertThat(JobHeader.read(null).key()).startsWith("texto:");
    }
}
