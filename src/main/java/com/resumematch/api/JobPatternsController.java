package com.resumematch.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumematch.log.JobMemory;
import com.resumematch.log.JobPatterns;

/**
 * Devolve o que as vagas da memoria mais pedem.
 */
@RestController
@RequestMapping("/api")
public class JobPatternsController {

    private final JobMemory jobMemory;

    public JobPatternsController(JobMemory jobMemory) {
        this.jobMemory = jobMemory;
    }

    @GetMapping("/patterns")
    public JobPatterns patterns() {
        return jobMemory.patterns();
    }
}
