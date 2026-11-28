package com.resumematch.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Nos testes a memoria vem desligada (src/test/resources/application.properties). */
@SpringBootTest
@AutoConfigureMockMvc
class JobPatternsDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void memoriaDesligadaDevolvePadroesVaziosSemErro() throws Exception {
        mockMvc.perform(get("/api/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.jobs").value(0));
    }
}
