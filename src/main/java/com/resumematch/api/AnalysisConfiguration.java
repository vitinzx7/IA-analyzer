package com.resumematch.api;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.resumematch.analysis.AnalysisLog;
import com.resumematch.analysis.AnalysisService;
import com.resumematch.dictionary.TechnologyDictionary;
import com.resumematch.feedback.FeedbackStore;
import com.resumematch.log.AnalysisCsvLogger;
import com.resumematch.log.JobMemory;
import com.resumematch.matcher.JobRequirementExtractor;
import com.resumematch.matcher.LocalSummaryGenerator;
import com.resumematch.matcher.RequirementMatcher;
import com.resumematch.matcher.ScoreCalculator;
import com.resumematch.matcher.SentenceSplitter;
import com.resumematch.matcher.SummaryGenerator;
import com.resumematch.pdf.PdfTextExtractor;

@Configuration
public class AnalysisConfiguration {

    @Bean
    public PdfTextExtractor pdfTextExtractor() {
        return new PdfTextExtractor();
    }

    @Bean
    public TechnologyDictionary technologyDictionary() {
        return new TechnologyDictionary();
    }

    @Bean
    public SentenceSplitter sentenceSplitter() {
        return new SentenceSplitter();
    }

    @Bean
    public JobRequirementExtractor jobRequirementExtractor(TechnologyDictionary dictionary) {
        return new JobRequirementExtractor(dictionary);
    }

    @Bean
    public RequirementMatcher requirementMatcher(TechnologyDictionary dictionary) {
        return new RequirementMatcher(dictionary);
    }

    @Bean
    public ScoreCalculator scoreCalculator() {
        return new ScoreCalculator();
    }

    @Bean
    public SummaryGenerator summaryGenerator() {
        return new LocalSummaryGenerator();
    }

    @Bean
    public AnalysisCsvLogger analysisCsvLogger(
            @Value("${resumematch.csv.path:analises.csv}") String path,
            @Value("${resumematch.csv.enabled:true}") boolean enabled) {
        return new AnalysisCsvLogger(Path.of(path), enabled);
    }

    @Bean
    public JobMemory jobMemory(
            @Value("${resumematch.jobs.path:vagas.csv}") String path,
            @Value("${resumematch.jobs.enabled:true}") boolean enabled) {
        return new JobMemory(Path.of(path), enabled);
    }

    @Bean
    public FeedbackStore feedbackStore(
            @Value("${resumematch.feedback.path:feedback.csv}") String path,
            @Value("${resumematch.feedback.enabled:true}") boolean enabled) {
        return new FeedbackStore(Path.of(path), enabled);
    }

    @Bean
    public AnalysisService analysisService(
            PdfTextExtractor pdfTextExtractor,
            SentenceSplitter sentenceSplitter,
            JobRequirementExtractor jobRequirementExtractor,
            RequirementMatcher requirementMatcher,
            ScoreCalculator scoreCalculator,
            SummaryGenerator summaryGenerator,
            AnalysisCsvLogger analysisCsvLogger,
            JobMemory jobMemory) {
        // Cada registro trata a propria falha, entao um nao impede o outro.
        AnalysisLog logs = (result, jobText) -> {
            analysisCsvLogger.log(result, jobText);
            jobMemory.log(result, jobText);
        };
        return new AnalysisService(
                pdfTextExtractor,
                sentenceSplitter,
                jobRequirementExtractor,
                requirementMatcher,
                scoreCalculator,
                summaryGenerator,
                logs,
                jobMemory);
    }
}
