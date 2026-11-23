package com.resumematch.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.resumematch.analysis.AnalysisException;
import com.resumematch.analysis.AnalysisService;
import com.resumematch.matcher.AnalysisResult;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResult analyze(
            @RequestPart(value = "resume", required = false) MultipartFile resume,
            @RequestParam(value = "job", required = false) String job) {
        PdfUploadValidator.validate(resume);
        String origin = resume.getOriginalFilename() == null
                ? "arquivo enviado"
                : resume.getOriginalFilename();
        try (InputStream input = resume.getInputStream()) {
            return analysisService.analyzePdf(input, origin, job);
        } catch (IOException e) {
            throw new AnalysisException("Nao foi possivel ler o PDF enviado.", e);
        }
    }

    @PostMapping(value = "/analyze/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResult analyzeText(@RequestBody(required = false) AnalyzeTextRequest request) {
        if (request == null) {
            throw new AnalysisException("O corpo da requisicao esta vazio.");
        }
        return analysisService.analyze(request.resume(), request.job());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
