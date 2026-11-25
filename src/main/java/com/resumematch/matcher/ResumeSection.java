package com.resumematch.matcher;

/**
 * Secao do curriculo de onde a evidencia veio.
 *
 * O peso decide qual evidencia mostrar quando o termo aparece em mais de um
 * lugar: projeto e experiencia mostram uso; curso e formacao mostram estudo;
 * competencias e resumo so declaram; o cabecalho (nome, titulo, contato) nao
 * prova nada. Menor e melhor.
 */
public enum ResumeSection {
    EXPERIENCE(0),
    PROJECTS(0),
    COURSES(1),
    EDUCATION(1),
    OTHER(2),
    SKILLS(3),
    LANGUAGES(3),
    SUMMARY(4),
    HEADER(5);

    private final int rank;

    ResumeSection(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
