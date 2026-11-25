package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProfileEvidenceTest {

    private static final String RESUME = """
            RESUMO
            Estudante de TI, falo um pouco de espanhol.
            FORMAÇÃO ACADÊMICA
            Tecnólogo em Análise e Desenvolvimento de Sistemas, Universidade Católica.
            IDIOMAS
            Inglês - Intermediário (B1)
            """;

    private final SentenceSplitter splitter = new SentenceSplitter();

    private List<UnevaluatedItem> attach(UnevaluatedItem... items) {
        return ProfileEvidence.attach(List.of(items), RESUME, splitter.split(RESUME));
    }

    @Test
    void achaAFormacaoNaSecaoDeFormacao() {
        UnevaluatedItem item = attach(new UnevaluatedItem("Cursando ensino superior em TI.",
                JobSection.REQUIREMENTS, UnevaluatedItem.Reason.EDUCATION)).getFirst();

        assertThat(item.resumeEvidence())
                .isEqualTo("Tecnólogo em Análise e Desenvolvimento de Sistemas, Universidade Católica.");
    }

    @Test
    void achaOIdiomaPedidoPelaVaga() {
        UnevaluatedItem ingles = attach(new UnevaluatedItem("Inglês técnico para leitura.",
                JobSection.PREFERRED, UnevaluatedItem.Reason.LANGUAGE)).getFirst();
        UnevaluatedItem espanhol = attach(new UnevaluatedItem("Espanhol intermediário.",
                JobSection.PREFERRED, UnevaluatedItem.Reason.LANGUAGE)).getFirst();
        UnevaluatedItem frances = attach(new UnevaluatedItem("Francês avançado.",
                JobSection.PREFERRED, UnevaluatedItem.Reason.LANGUAGE)).getFirst();

        assertThat(ingles.resumeEvidence()).isEqualTo("Inglês - Intermediário (B1)");
        assertThat(espanhol.resumeEvidence()).isEqualTo("Estudante de TI, falo um pouco de espanhol.");
        assertThat(frances.resumeEvidence()).isNull();
    }

    @Test
    void trechoSemTermoNaoGanhaEvidencia() {
        UnevaluatedItem item = attach(new UnevaluatedItem("Boa comunicação.",
                JobSection.REQUIREMENTS, UnevaluatedItem.Reason.NO_KNOWN_TERM)).getFirst();

        assertThat(item.resumeEvidence()).isNull();
    }
}
