package com.resumematch.feedback;

import static com.resumematch.matcher.MatchStatus.MATCH;
import static com.resumematch.matcher.MatchStatus.MISSING;
import static com.resumematch.matcher.MatchStatus.PARTIAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.resumematch.analysis.AnalysisException;
import com.resumematch.matcher.MatchStatus;

class FeedbackMetricsTest {

    private Feedback mark(String requirement, MatchStatus program, MatchStatus correct) {
        return new Feedback("a1", requirement, program, correct);
    }

    @Test
    void calculaAcertoPrecisaoERevocacao() {
        FeedbackMetrics metrics = FeedbackMetrics.of(List.of(
                mark("Java", MATCH, MATCH),
                mark("Docker", MATCH, PARTIAL),
                mark("Git", PARTIAL, PARTIAL),
                mark("AWS", MISSING, MISSING),
                mark("Linux", MISSING, MATCH)));

        assertThat(metrics.total()).isEqualTo(5);
        assertThat(metrics.correct()).isEqualTo(3);
        assertThat(metrics.accuracy()).isEqualTo(60.0);

        FeedbackMetrics.StatusMetrics match = metrics.byStatus().get(MATCH);
        assertThat(match.marked()).isEqualTo(2);
        assertThat(match.precision()).isEqualTo(50.0);
        assertThat(match.expected()).isEqualTo(2);
        assertThat(match.recall()).isEqualTo(50.0);

        FeedbackMetrics.StatusMetrics partial = metrics.byStatus().get(PARTIAL);
        assertThat(partial.precision()).isEqualTo(100.0);
        assertThat(partial.recall()).isEqualTo(50.0);
    }

    @Test
    void ultimaMarcacaoDoMesmoRequisitoNaMesmaAnaliseVale() {
        FeedbackMetrics metrics = FeedbackMetrics.of(List.of(
                mark("Java", MATCH, PARTIAL),
                mark("Java", MATCH, MATCH),
                new Feedback("a2", "Java", MATCH, PARTIAL)));

        assertThat(metrics.total()).isEqualTo(2);
        assertThat(metrics.correct()).isEqualTo(1);
    }

    @Test
    void semMarcacaoAsProporcoesFicamNulas() {
        FeedbackMetrics metrics = FeedbackMetrics.of(List.of());

        assertThat(metrics.total()).isZero();
        assertThat(metrics.accuracy()).isNull();
        assertThat(metrics.byStatus().get(MATCH).precision()).isNull();
        assertThat(metrics.byStatus().get(MATCH).recall()).isNull();
    }

    @Test
    void recusaMarcacaoInvalida() {
        assertThatThrownBy(() -> new Feedback("id com espaco", "Java", MATCH, MATCH))
                .isInstanceOf(AnalysisException.class);
        assertThatThrownBy(() -> new Feedback("a1", " ", MATCH, MATCH))
                .isInstanceOf(AnalysisException.class);
        assertThatThrownBy(() -> new Feedback("a1", "Java\nAWS", MATCH, MATCH))
                .isInstanceOf(AnalysisException.class);
        assertThatThrownBy(() -> new Feedback("a1", "Java", null, MATCH))
                .isInstanceOf(AnalysisException.class);
    }
}
