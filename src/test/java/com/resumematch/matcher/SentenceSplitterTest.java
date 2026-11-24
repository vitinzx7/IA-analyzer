package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SentenceSplitterTest {

    private final SentenceSplitter splitter = new SentenceSplitter();

    private List<String> sentences(String text) {
        return splitter.split(text).stream().map(Sentence::text).toList();
    }

    @Test
    void separaFrasesPorPonto() {
        assertThat(sentences("Desenvolvi uma API. Tambem usei Docker."))
                .containsExactly("Desenvolvi uma API.", "Tambem usei Docker.");
    }

    @Test
    void juntaLinhaQuebradaNoMeioDaFrase() {
        assertThat(sentences("Desenvolvi uma API com\nJava e Spring Boot."))
                .containsExactly("Desenvolvi uma API com Java e Spring Boot.");
    }

    @Test
    void naoJuntaQuandoALinhaTerminaComPonto() {
        assertThat(sentences("Desenvolvi uma API.\nUsei Docker."))
                .containsExactly("Desenvolvi uma API.", "Usei Docker.");
    }

    @Test
    void naoJuntaBulletComOutroBullet() {
        assertThat(sentences("• Desenvolvi a API\n• Implementei o deploy"))
                .containsExactly("Desenvolvi a API", "Implementei o deploy");
    }

    @Test
    void juntaContinuacaoDeUmBullet() {
        assertThat(sentences("• Desenvolvi a API\ncom Java e Spring Boot"))
                .containsExactly("Desenvolvi a API com Java e Spring Boot");
    }

    @Test
    void naoJuntaCabecalhoDeSecao() {
        assertThat(sentences("EXPERIENCIA PROFISSIONAL\nDesenvolvi uma API."))
                .containsExactly("EXPERIENCIA PROFISSIONAL", "Desenvolvi uma API.");
    }

    @Test
    void separaPorPontoEVirgula() {
        assertThat(sentences("Usei Java; usei Docker."))
                .containsExactly("Usei Java;", "usei Docker.");
    }

    @Test
    void mantemListaComDoisPontosComoUmaFrase() {
        assertThat(sentences("Tecnologias: Java, Docker, Git"))
                .containsExactly("Tecnologias: Java, Docker, Git");
    }

    @Test
    void naoQuebraNumeroDecimal() {
        assertThat(sentences("Melhorei o tempo em 3.5 segundos."))
                .containsExactly("Melhorei o tempo em 3.5 segundos.");
    }

    @Test
    void naoQuebraNomeDeTecnologiaComPonto() {
        assertThat(sentences("Desenvolvi com Node.js e ASP.NET."))
                .containsExactly("Desenvolvi com Node.js e ASP.NET.");
    }

    @Test
    void guardaIntervaloDoTextoOriginal() {
        String text = "Desenvolvi uma API.\nUsei Docker.";
        List<Sentence> result = splitter.split(text);

        assertThat(result).hasSize(2);
        assertThat(text.substring(result.get(0).start(), result.get(0).end()))
                .contains("Desenvolvi uma API.");
        assertThat(text.substring(result.get(1).start(), result.get(1).end()))
                .contains("Usei Docker.");
    }

    @Test
    void ignoraTextoVazio() {
        assertThat(splitter.split("")).isEmpty();
        assertThat(splitter.split("   ")).isEmpty();
        assertThat(splitter.split(null)).isEmpty();
    }

    @Test
    void linhaDeContatoNaoSeJuntaComOutras() {
        assertThat(sentences("Victor Douglas\nEstudante de TI | Redes\nvictor@email.com"))
                .containsExactly("Victor Douglas Estudante de TI | Redes", "victor@email.com");
        assertThat(sentences("Victor Douglas\n(61) 99999-9999\nBrasilia"))
                .containsExactly("Victor Douglas", "(61) 99999-9999", "Brasilia");
    }

    @Test
    void arrobaEBarraVerticalNaoInterrompemAFrase() {
        assertThat(sentences("• Implementei endpoints com @RestController e\nSpring Boot no backend."))
                .containsExactly("Implementei endpoints com @RestController e Spring Boot no backend.");
        assertThat(sentences("• Desenvolvi a API em Java | camada de servico e\nPostgreSQL como banco."))
                .containsExactly("Desenvolvi a API em Java | camada de servico e PostgreSQL como banco.");
    }
}
