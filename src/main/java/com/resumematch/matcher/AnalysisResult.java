package com.resumematch.matcher;

import java.util.List;

public record AnalysisResult(double score, List<RequirementMatch> requirements, String summary) {

    public AnalysisResult {
        requirements = List.copyOf(requirements);
    }
}
