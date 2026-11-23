package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

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
}
