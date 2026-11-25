package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResumeSectionsTest {

    private static final String RESUME = """
            Fulano de Tal
            Estudante de TI | Suporte Técnico
            RESUMO
            Tenho interesse em infraestrutura.
            COMPETÊNCIAS TÉCNICAS
            Ferramentas: Docker e Git.
            Experiência com Java em projetos
            PROJETOS
            Desenvolvi uma API.
            Cursos complementares:
            Redes de Computadores (40h)
            """;

    private ResumeSection sectionOf(String excerpt) {
        return ResumeSections.read(RESUME).sectionAt(RESUME.indexOf(excerpt));
    }

    @Test
    void textoAntesDoPrimeiroTituloEhCabecalho() {
        assertThat(sectionOf("Estudante de TI")).isEqualTo(ResumeSection.HEADER);
    }

    @Test
    void reconheceTitulosComQualificacao() {
        assertThat(sectionOf("Tenho interesse")).isEqualTo(ResumeSection.SUMMARY);
        assertThat(sectionOf("Ferramentas: Docker")).isEqualTo(ResumeSection.SKILLS);
        assertThat(sectionOf("Desenvolvi uma API")).isEqualTo(ResumeSection.PROJECTS);
        assertThat(sectionOf("Redes de Computadores (40h)")).isEqualTo(ResumeSection.COURSES);
    }

    @Test
    void linhaQueComecaComPalavraDeTituloNaoMudaASecao() {
        assertThat(sectionOf("Experiência com Java")).isEqualTo(ResumeSection.SKILLS);
    }

    @Test
    void curriculoSemTituloFicaTodoEmOther() {
        String text = "Desenvolvi uma API em Java.\nTecnologias usadas: Docker.";
        assertThat(ResumeSections.read(text).sectionAt(0)).isEqualTo(ResumeSection.OTHER);
        assertThat(ResumeSections.read(text).sectionAt(text.length() - 1)).isEqualTo(ResumeSection.OTHER);
    }
}
