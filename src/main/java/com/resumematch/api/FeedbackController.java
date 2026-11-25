package com.resumematch.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumematch.analysis.AnalysisException;
import com.resumematch.feedback.Feedback;
import com.resumematch.feedback.FeedbackMetrics;
import com.resumematch.feedback.FeedbackStore;

/**
 * Recebe as marcacoes de certo ou errado e devolve o acerto acumulado.
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackStore store;

    public FeedbackController(FeedbackStore store) {
        this.store = store;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public FeedbackMetrics record(@RequestBody(required = false) Feedback feedback) {
        if (feedback == null) {
            throw new AnalysisException("O corpo da requisicao esta vazio.");
        }
        if (!store.enabled()) {
            throw new AnalysisException(
                    "O registro de marcacoes esta desligado (resumematch.feedback.enabled=false).");
        }
        store.record(feedback);
        return summary();
    }

    @GetMapping("/summary")
    public FeedbackMetrics summary() {
        return FeedbackMetrics.of(store.all());
    }
}
