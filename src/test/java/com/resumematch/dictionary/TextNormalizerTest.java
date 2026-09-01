package com.resumematch.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextNormalizerTest {

    @Test
    void deixaMinusculo() {
        assertThat(TextNormalizer.normalize("Spring Boot")).isEqualTo("spring boot");
    }

    @Test
    void removeAcentos() {
        assertThat(TextNormalizer.normalize("Análise e Configuração"))
                .isEqualTo("analise e configuracao");
    }

    @ParameterizedTest
    @ValueSource(strings = {"C#", "C++", ".NET", "Node.js", "CI/CD", "TCP/IP"})
    void preservaPontuacaoDosCasosEspeciais(String termo) {
        assertThat(TextNormalizer.normalize(termo)).isEqualTo(termo.toLowerCase());
    }

    @Test
    void normalizaIgualParaTextoComposicaoNfcENfd() {
        String nfc = "Manutenção";
        String nfd = Normalizer.normalize(nfc, Normalizer.Form.NFD);

        assertThat(nfd).hasSizeGreaterThan(nfc.length());
        assertThat(TextNormalizer.normalize(nfd)).isEqualTo(TextNormalizer.normalize(nfc));
    }

    @Test
    void mapaDeIndicesRecuperaOTrechoOriginalEmTextoComposto() {
        String original = "Manutenção de Docker";

        assertRecuperaTrecho(original, "docker", "Docker");
    }

    @Test
    void mapaDeIndicesRecuperaOTrechoOriginalEmTextoDecomposto() {
        String original = Normalizer.normalize("Manutenção de Docker", Normalizer.Form.NFD);

        assertRecuperaTrecho(original, "docker", "Docker");
    }

    @Test
    void mapaDeIndicesCobreOAcentoCombinanteQueSegueALetra() {
        String original = Normalizer.normalize("Manutenção", Normalizer.Form.NFD);

        // o trecho recuperado precisa carregar tambem os acentos combinantes,
        // que ocupam posicao no original e nenhuma no texto normalizado
        assertRecuperaTrecho(original, "manutencao", original);
    }

    private void assertRecuperaTrecho(String original, String procurado, String esperado) {
        TextNormalizer.Normalized mapped = TextNormalizer.map(original);
        int start = mapped.value().indexOf(procurado);

        assertThat(start).isNotNegative();
        int originalStart = mapped.originalStart(start);
        int originalEnd = mapped.originalEnd(start + procurado.length());

        assertThat(original.substring(originalStart, originalEnd)).isEqualTo(esperado);
    }

    @Test
    void aceitaNulo() {
        assertThat(TextNormalizer.normalize(null)).isEmpty();
    }
}
