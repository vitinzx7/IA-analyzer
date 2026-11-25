package com.resumematch.analysis;

import java.io.InputStream;
import java.util.List;

import com.resumematch.matcher.AnalysisResult;
import com.resumematch.matcher.JobExtraction;
import com.resumematch.matcher.JobRequirementExtractor;
import com.resumematch.matcher.ProfileEvidence;
import com.resumematch.matcher.RequirementMatch;
import com.resumematch.matcher.RequirementMatcher;
import com.resumematch.matcher.ScoreCalculator;
import com.resumematch.matcher.Sentence;
import com.resumematch.matcher.SentenceSplitter;
import com.resumematch.matcher.SummaryGenerator;
import com.resumematch.pdf.HiddenTextReport;
import com.resumematch.pdf.PdfText;
import com.resumematch.pdf.PdfTextExtractor;

/**
 * Orquestra a analise: curriculo e vaga entram, score e evidencias saem.
 */
public final class AnalysisService {

    static final int MAX_RESUME_CHARS = 200_000;
    static final int MAX_JOB_CHARS = 50_000;

    private final PdfTextExtractor pdfTextExtractor;
    private final SentenceSplitter sentenceSplitter;
    private final JobRequirementExtractor requirementExtractor;
    private final RequirementMatcher matcher;
    private final ScoreCalculator scoreCalculator;
    private final SummaryGenerator summaryGenerator;
    private final AnalysisLog analysisLog;

    public AnalysisService(PdfTextExtractor pdfTextExtractor,
            SentenceSplitter sentenceSplitter,
            JobRequirementExtractor requirementExtractor,
            RequirementMatcher matcher,
            ScoreCalculator scoreCalculator,
            SummaryGenerator summaryGenerator) {
        this(pdfTextExtractor, sentenceSplitter, requirementExtractor, matcher,
                scoreCalculator, summaryGenerator, AnalysisLog.NONE);
    }

    public AnalysisService(PdfTextExtractor pdfTextExtractor,
            SentenceSplitter sentenceSplitter,
            JobRequirementExtractor requirementExtractor,
            RequirementMatcher matcher,
            ScoreCalculator scoreCalculator,
            SummaryGenerator summaryGenerator,
            AnalysisLog analysisLog) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.sentenceSplitter = sentenceSplitter;
        this.requirementExtractor = requirementExtractor;
        this.matcher = matcher;
        this.scoreCalculator = scoreCalculator;
        this.summaryGenerator = summaryGenerator;
        this.analysisLog = analysisLog == null ? AnalysisLog.NONE : analysisLog;
    }

    public AnalysisResult analyze(String resumeText, String jobText) {
        return analyze(resumeText, jobText, null);
    }

    public AnalysisResult analyzePdf(InputStream pdf, String origin, String jobText) {
        PdfText resume = pdfTextExtractor.extractChecked(pdf, origin);
        return analyze(resume.text(), jobText, resume.report());
    }

    private AnalysisResult analyze(String resumeText, String jobText, HiddenTextReport pdfCheck) {
        validate(resumeText, jobText);

        List<Sentence> sentences = sentenceSplitter.split(resumeText);
        JobExtraction job = requirementExtractor.read(jobText);
        List<RequirementMatch> matches = matcher.matchRequirements(job.requirements(), resumeText, sentences);
        double score = scoreCalculator.calculate(matches);
        String summary = summaryGenerator.generate(matches, score, job) + hiddenTextNote(pdfCheck);
        AnalysisResult result = new AnalysisResult(matches.isEmpty() ? null : score, matches, summary,
                ProfileEvidence.attach(job.unevaluated(), resumeText, sentences), job.unknownTerms(),
                pdfCheck);
        analysisLog.log(result);
        return result;
    }

    /** Frase sobre texto escondido no PDF, ou vazio. */
    private static String hiddenTextNote(HiddenTextReport pdfCheck) {
        if (pdfCheck == null) {
            return "";
        }
        if (pdfCheck.removedHiddenText()) {
            return " O PDF tinha texto invisivel, que ficou fora da analise.";
        }
        if (pdfCheck.hasSuspiciousText()) {
            return " O PDF tem texto branco, minusculo ou fora da pagina; confira o aviso.";
        }
        return "";
    }

    private void validate(String resumeText, String jobText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new AnalysisException("O curriculo esta vazio.");
        }
        if (jobText == null || jobText.isBlank()) {
            throw new AnalysisException("A descricao da vaga esta vazia.");
        }
        if (resumeText.length() > MAX_RESUME_CHARS) {
            throw new AnalysisException("O texto do curriculo e muito longo.");
        }
        if (jobText.length() > MAX_JOB_CHARS) {
            throw new AnalysisException("A descricao da vaga e muito longa.");
        }
    }
}
