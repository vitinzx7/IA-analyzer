package com.resumematch.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebUiIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void entregaAPaginaComOFormulario() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("id=\"analysis-form\"")
                .contains("Seu currículo combina com esta vaga?");
    }

    @Test
    void entregaOsRecursosDaPagina() throws Exception {
        assertThat(get("/styles.css").statusCode()).isEqualTo(200);
        assertThat(get("/app.js").body()).contains("/api/analyze");
    }

    @Test
    void paginaAceitaPdfOuTextoColado() throws Exception {
        assertThat(get("/").body())
                .contains("id=\"resume\"")
                .contains("id=\"resume-text\"");
        assertThat(get("/app.js").body()).contains("/api/analyze/text");
    }

    @Test
    void scriptMostraDiferencialConjuntoEONaoAvaliado() throws Exception {
        assertThat(get("/app.js").body())
                .contains("data.unevaluated")
                .contains("data.unknownTerms")
                .contains("item.components")
                .contains("\"PREFERRED\"");
    }

    @Test
    void paginaMostraASimulacaoDeMelhora() throws Exception {
        assertThat(get("/").body())
                .contains("id=\"simulation\"")
                .contains("Simulação de melhora");
        assertThat(get("/app.js").body())
                .contains("data.improvement")
                .contains("scoreWithAll")
                .contains("Lacuna real: precisa ter, não só escrever");
    }

    @Test
    void scriptEnviaMarcacaoDeCertoOuErrado() throws Exception {
        assertThat(get("/").body()).contains("id=\"feedback-summary\"");
        assertThat(get("/app.js").body())
                .contains("/api/feedback")
                .contains("/api/feedback/summary")
                .contains("analysisId");
    }

    @Test
    void telaChamaONumeroDeCoberturaTecnica() throws Exception {
        assertThat(get("/").body())
                .contains("Cobertura técnica da vaga")
                .contains("id=\"required-gap\"")
                .doesNotContain(">Score<");
        assertThat(get("/app.js").body())
                .contains("evidenceSection")
                .contains("resumeEvidence");
    }

    @Test
    void telaAvisaSobreTextoEscondidoNoPdf() throws Exception {
        assertThat(get("/").body()).contains("id=\"pdf-warning\"");
        assertThat(get("/app.js").body())
                .contains("data.pdfCheck")
                .contains("invisibleChars")
                .contains("scannedLayer");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
