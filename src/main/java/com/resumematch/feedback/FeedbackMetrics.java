package com.resumematch.feedback;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.resumematch.matcher.MatchStatus;

/**
 * Quanto o programa acertou, segundo as marcacoes.
 *
 * Para cada status:
 * <ul>
 * <li>precisao: das vezes em que o programa disse X, quantas estavam certas;</li>
 * <li>revocacao: das vezes em que o certo era X, quantas o programa achou.</li>
 * </ul>
 * MATCH dado sem merecer derruba a precisao de MATCH (falso positivo); MISSING
 * com evidencia no curriculo derruba a revocacao de MATCH e PARTIAL (falso
 * negativo). Uma proporcao sem nenhum caso fica null, e nao 0.
 *
 * Se o mesmo requisito da mesma analise for marcado mais de uma vez, vale a
 * ultima marcacao.
 */
public record FeedbackMetrics(int total, int correct, Double accuracy,
        Map<MatchStatus, StatusMetrics> byStatus) {

    public record StatusMetrics(int marked, int markedCorrect, Double precision,
            int expected, int found, Double recall) {
    }

    public static FeedbackMetrics of(List<Feedback> feedback) {
        Map<String, Feedback> latest = new LinkedHashMap<>();
        for (Feedback item : feedback) {
            String key = item.analysisId() + "\u0000" + item.requirement();
            latest.remove(key);
            latest.put(key, item);
        }
        List<Feedback> marks = List.copyOf(latest.values());

        int correct = (int) marks.stream().filter(Feedback::correct).count();
        Map<MatchStatus, StatusMetrics> byStatus = new EnumMap<>(MatchStatus.class);
        for (MatchStatus status : MatchStatus.values()) {
            int marked = (int) marks.stream().filter(m -> m.programStatus() == status).count();
            int markedCorrect = (int) marks.stream()
                    .filter(m -> m.programStatus() == status && m.correct()).count();
            int expected = (int) marks.stream().filter(m -> m.correctStatus() == status).count();
            byStatus.put(status, new StatusMetrics(marked, markedCorrect, ratio(markedCorrect, marked),
                    expected, markedCorrect, ratio(markedCorrect, expected)));
        }
        return new FeedbackMetrics(marks.size(), correct, ratio(correct, marks.size()), byStatus);
    }

    private static Double ratio(int part, int whole) {
        if (whole == 0) {
            return null;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }
}
