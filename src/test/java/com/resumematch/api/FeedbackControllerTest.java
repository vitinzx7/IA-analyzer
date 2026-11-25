package com.resumematch.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerTest {

    @TempDir
    static Path dir;

    @DynamicPropertySource
    static void feedbackFile(DynamicPropertyRegistry registry) {
        registry.add("resumematch.feedback.enabled", () -> "true");
        registry.add("resumematch.feedback.path", () -> dir.resolve("feedback.csv").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void gravaMarcacaoEDevolveOAcerto() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"analysisId":"teste-1","requirement":"Java",
                         "programStatus":"MATCH","correctStatus":"MATCH"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.correct").value(1));

        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"analysisId":"teste-1","requirement":"Docker",
                         "programStatus":"MATCH","correctStatus":"PARTIAL"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.accuracy").value(50.0))
                .andExpect(jsonPath("$.byStatus.MATCH.precision").value(50.0));

        mockMvc.perform(get("/api/feedback/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void recusaMarcacaoInvalidaComMensagemDaValidacao() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"analysisId":"id invalido","requirement":"Java",
                         "programStatus":"MATCH","correctStatus":"MATCH"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Identificador da analise invalido."));

        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"analysisId":"a1","requirement":"Java",
                         "programStatus":"TALVEZ","correctStatus":"MATCH"}
                        """))
                .andExpect(status().isBadRequest());
    }
}
