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
class JobPatternsControllerTest {

    @TempDir
    static Path dir;

    @DynamicPropertySource
    static void jobsFile(DynamicPropertyRegistry registry) {
        registry.add("resumematch.jobs.enabled", () -> "true");
        registry.add("resumematch.jobs.path", () -> dir.resolve("vagas.csv").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void segundaAnaliseDaMesmaVagaAvisaEPadroesContamUmaVez() throws Exception {
        String body = """
                {"resume": "Desenvolvi uma API em Java.",
                 "job": "Dev Java\\nEmpresa X\\nhttps://www.linkedin.com/jobs/view/99/\\n\\nRequisitos: Java e Docker."}
                """;

        mockMvc.perform(post("/api/analyze/text").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous").doesNotExist());
        mockMvc.perform(post("/api/analyze/text").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.count").value(1))
                .andExpect(jsonPath("$.previous.lastCoverage").value(50.0));

        mockMvc.perform(get("/api/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.jobs").value(1))
                .andExpect(jsonPath("$.analyses").value(2))
                .andExpect(jsonPath("$.requirements[0].jobs").value(1))
                .andExpect(jsonPath("$.requirements[?(@.name == 'Docker')].required").value(1));
    }
}
