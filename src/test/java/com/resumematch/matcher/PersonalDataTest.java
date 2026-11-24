package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonalDataTest {

    @Test
    void mascaraEmail() {
        assertThat(PersonalData.mask("Victor | Desenvolvedor Java | victor@email.com"))
                .isEqualTo("Victor | Desenvolvedor Java | [email]");
        assertThat(PersonalData.mask("contato: nome.sobrenome+cv@empresa.com.br"))
                .isEqualTo("contato: [email]");
    }

    @Test
    void mascaraTelefoneComDdd() {
        assertThat(PersonalData.mask("Tel: (61) 99999-9999")).isEqualTo("Tel: [telefone]");
        assertThat(PersonalData.mask("Tel: +55 61 99999-9999")).isEqualTo("Tel: [telefone]");
        assertThat(PersonalData.mask("Tel: 61 3333-4444")).isEqualTo("Tel: [telefone]");
        assertThat(PersonalData.mask("Tel: 61 9 8888 7777")).isEqualTo("Tel: [telefone]");
        assertThat(PersonalData.mask("Tel: 61999998888")).isEqualTo("Tel: [telefone]");
    }

    @Test
    void naoMascaraAnoVersaoNemPeriodo() {
        assertThat(PersonalData.mask("Conclusão prevista: 2028")).isEqualTo("Conclusão prevista: 2028");
        assertThat(PersonalData.mask("Desenvolvi em Java 21")).isEqualTo("Desenvolvi em Java 21");
        assertThat(PersonalData.mask("Graduação 2026–2028")).isEqualTo("Graduação 2026–2028");
        assertThat(PersonalData.mask("Graduação 2026-2028")).isEqualTo("Graduação 2026-2028");
        assertThat(PersonalData.mask("Estágio 01/2024 - 12/2025")).isEqualTo("Estágio 01/2024 - 12/2025");
        assertThat(PersonalData.mask("Servidor 192.168.10.100")).isEqualTo("Servidor 192.168.10.100");
    }

    @Test
    void arrobaSoltaNaoEhEmail() {
        assertThat(PersonalData.mask("Implementei endpoints com @RestController"))
                .isEqualTo("Implementei endpoints com @RestController");
    }

    @Test
    void reconheceLinhaDeContatoSoPorPadraoReal() {
        assertThat(PersonalData.isContactLine("victor@email.com")).isTrue();
        assertThat(PersonalData.isContactLine("(61) 99999-9999")).isTrue();
        assertThat(PersonalData.isContactLine("linkedin.com/in/fulano")).isTrue();
        assertThat(PersonalData.isContactLine("github.com/fulano")).isTrue();
        assertThat(PersonalData.isContactLine("https://fulano.dev")).isTrue();

        assertThat(PersonalData.isContactLine("Implementei endpoints com @RestController e")).isFalse();
        assertThat(PersonalData.isContactLine("Estudante de TI | Redes")).isFalse();
        assertThat(PersonalData.isContactLine("Graduação 2026-2028")).isFalse();
    }
}
