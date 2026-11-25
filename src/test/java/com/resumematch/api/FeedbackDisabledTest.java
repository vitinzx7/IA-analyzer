package com.resumematch.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Os testes rodam com resumematch.feedback.enabled=false. */
@SpringBootTest
@AutoConfigureMockMvc
class FeedbackDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void avisaQueORegistroEstaDesligado() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"analysisId":"a1","requirement":"Java",
                         "programStatus":"MATCH","correctStatus":"MATCH"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "O registro de marcacoes esta desligado (resumematch.feedback.enabled=false)."));

        mockMvc.perform(get("/api/feedback/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
