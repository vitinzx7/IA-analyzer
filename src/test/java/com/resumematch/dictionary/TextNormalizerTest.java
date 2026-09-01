package com.resumematch.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

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
    void preservaOComprimentoParaQueOsIndicesSirvamNoTextoOriginal() {
        String original = "Análise, Configuração, Manutenção e Automação de Serviços";

        assertThat(TextNormalizer.normalize(original)).hasSameSizeAs(original);
    }

    @Test
    void aceitaNulo() {
        assertThat(TextNormalizer.normalize(null)).isEmpty();
    }
}
