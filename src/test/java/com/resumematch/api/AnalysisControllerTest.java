package com.resumematch.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthRespondeOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void analisaPdfMultipart() throws Exception {
        mockMvc.perform(multipart("/api/analyze")
                .file(new MockMultipartFile("resume", "cv.pdf", "application/pdf", pdfBytes()))
                .param("job", "Java, Docker, AWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(66.7))
                .andExpect(jsonPath("$.requirements[0].name").value("Java"))
                .andExpect(jsonPath("$.requirements[0].status").value("MATCH"))
                .andExpect(jsonPath("$.requirements[2].status").value("MISSING"))
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void semRequisitoReconhecidoMandaScoreNulo() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resume":"Desenvolvi uma API em Java.",
                         "job":"Cozinheiro com experiencia em restaurante"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(nullValue()))
                .andExpect(jsonPath("$.requirements").isEmpty());
    }

    @Test
    void analisaTextoJson() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resume":"Desenvolvi uma API em Java.\\nTecnologias: Docker.",
                         "job":"Java, Docker, AWS"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(50.0))
                .andExpect(jsonPath("$.requirements[0].status").value("MATCH"))
                .andExpect(jsonPath("$.requirements[1].status").value("PARTIAL"))
                .andExpect(jsonPath("$.requirements[2].status").value("MISSING"));
    }

    @Test
    void mandaAPrioridadeDeCadaRequisito() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resume":"Desenvolvi uma API em Java.",
                         "job":"Requisitos: Java.\\nDiferenciais: AWS."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(66.7))
                .andExpect(jsonPath("$.requirements[0].priority").value("REQUIRED"))
                .andExpect(jsonPath("$.requirements[1].priority").value("PREFERRED"));
    }

    @Test
    void mandaASecaoDaEvidencia() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resume":"RESUMO\\nTecnologias: Java.\\nPROJETOS\\nDesenvolvi uma API em Java.",
                         "job":"Java e AWS"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].evidenceSection").value("PROJECTS"))
                .andExpect(jsonPath("$.requirements[1].evidenceSection").value(nullValue()));
    }

    @Test
    void mandaOQueNaoFoiAvaliado() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resume":"Desenvolvi uma API em Java.",
                         "job":"Requisitos:\\nJava.\\nInglês técnico.\\nFerramentas como Movidesk."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unevaluated[0].text").value("Inglês técnico."))
                .andExpect(jsonPath("$.unevaluated[0].section").value("REQUIREMENTS"))
                .andExpect(jsonPath("$.unevaluated[0].reason").value("LANGUAGE"))
                .andExpect(jsonPath("$.unevaluated[1].reason").value("NO_KNOWN_TERM"))
                .andExpect(jsonPath("$.unknownTerms[0]").value("Movidesk"));
    }

    @Test
    void mandaAsPartesDoConjunto() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resume":"Configurei o DNS do laboratorio.",
                         "job":"Protocolos (TCP/IP, DNS, DHCP)."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].name").value("TCP/IP + DNS + DHCP"))
                .andExpect(jsonPath("$.requirements[0].status").value("PARTIAL"))
                .andExpect(jsonPath("$.requirements[0].components[1].name").value("DNS"))
                .andExpect(jsonPath("$.requirements[0].components[1].status").value("MATCH"))
                .andExpect(jsonPath("$.requirements[0].components[2].status").value("MISSING"));
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O corpo da requisicao nao e um JSON valido."));
    }

    @Test
    void devolveNotFoundParaRotaInexistente() throws Exception {
        mockMvc.perform(get("/api/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("A rota solicitada nao existe."));
    }

    @Test
    void rejeitaArquivoQueNaoEhPdf() throws Exception {
        mockMvc.perform(multipart("/api/analyze")
                .file(new MockMultipartFile("resume", "cv.pdf", "application/pdf",
                        "nao e pdf".getBytes()))
                .param("job", "Java"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O arquivo enviado nao e um PDF valido."));
    }

    @Test
    void rejeitaVagaVazia() throws Exception {
        mockMvc.perform(multipart("/api/analyze")
                .file(new MockMultipartFile("resume", "cv.pdf", "application/pdf", pdfBytes()))
                .param("job", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaPdfSemTextoSuficiente() throws Exception {
        mockMvc.perform(multipart("/api/analyze")
                .file(new MockMultipartFile("resume", "cv.pdf", "application/pdf", pdfBytes("oi")))
                .param("job", "Java"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value(org.hamcrest.Matchers.containsString("caracteres")));
    }

    @Test
    void rejeitaVagaMuitoLongaNoEndpointDeTexto() throws Exception {
        String job = "x".repeat(50_001);

        mockMvc.perform(post("/api/analyze/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resume\":\"Desenvolvi uma API em Java.\",\"job\":\"" + job + "\"}"))
                .andExpect(status().isBadRequest());
    }

    private byte[] pdfBytes() throws Exception {
        return pdfBytes("Desenvolvi uma API em Java com Spring Boot e PostgreSQL. "
                + "Tambem utilizei Docker e Git no projeto. "
                + "Configurei o ambiente e escrevi os testes automatizados. "
                + "Mantive a documentacao atualizada durante o desenvolvimento.");
    }

    private byte[] pdfBytes(String content) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
