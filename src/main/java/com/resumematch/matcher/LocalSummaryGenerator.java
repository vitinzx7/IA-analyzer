package com.resumematch.matcher;

import java.util.List;
import java.util.Locale;

/**
 * Monta o resumo localmente, sem depender de servico externo.
 *
 * As sugestoes sao sempre condicionais: o sistema nao sabe o que a pessoa
 * realmente viveu, entao nunca manda inventar experiencia.
 */
public final class LocalSummaryGenerator implements SummaryGenerator {

    private static final int MAX_NAMES = 5;

    @Override
    public String generate(List<RequirementMatch> requirements, double score) {
        return generate(requirements, score, null);
    }

    @Override
    public String generate(List<RequirementMatch> requirements, double score, JobExtraction job) {
        String unevaluated = unevaluated(job);
        if (requirements == null || requirements.isEmpty()) {
            return "Nenhum requisito reconhecido na descricao da vaga. "
                    + "O dicionario atual nao cobre os termos desta vaga, entao o score nao foi calculado. "
                    + unevaluated
                    + "Requisitos fora do dicionario nao entram no calculo.";
        }

        List<RequirementMatch> matches = requirements.stream()
                .filter(r -> r.status() == MatchStatus.MATCH).toList();
        List<RequirementMatch> partials = requirements.stream()
                .filter(r -> r.status() == MatchStatus.PARTIAL).toList();
        List<RequirementMatch> missing = requirements.stream()
                .filter(r -> r.status() == MatchStatus.MISSING).toList();

        StringBuilder summary = new StringBuilder();
        summary.append("Cobertura tecnica ").append(formatScore(score)).append("/100: ")
                .append(matches.size()).append(" de ").append(requirements.size())
                .append(" requisitos reconhecidos com evidencia completa (")
                .append(matches.size()).append(" MATCH, ")
                .append(partials.size()).append(" PARTIAL, ")
                .append(missing.size()).append(" MISSING).");

        if (!missing.isEmpty()) {
            summary.append(" Nao encontrei evidencia para: ")
                    .append(names(missing)).append('.')
                    .append(" Se voce realmente tem experiencia com esses termos, ")
                    .append("considere deixar isso explicito no curriculo.");
        }

        if (!partials.isEmpty()) {
            summary.append(" Aparecem so em listas, sem uma frase de acao: ")
                    .append(names(partials)).append('.')
                    .append(" Se voce ja usou esses termos em projetos, descreva o que fez.");
        }

        if (missing.isEmpty() && partials.isEmpty()) {
            summary.append(" Todos os requisitos reconhecidos tem evidencia com acao no curriculo.");
        }

        if (requirements.stream().anyMatch(r -> r.priority() == RequirementPriority.PREFERRED)) {
            summary.append(" Diferenciais valem metade no calculo.");
        }

        summary.append(' ').append(unevaluated)
                .append("Requisitos fora do dicionario nao entram no calculo. ")
                .append("A cobertura tecnica nao e nota do candidato nem probabilidade de contratacao.");
        return summary.toString();
    }

    /** Frase sobre os trechos da vaga que ficaram de fora, ou vazio. */
    private String unevaluated(JobExtraction job) {
        int count = job == null ? 0 : job.unevaluated().size();
        if (count == 0) {
            return "";
        }
        return count + " trecho(s) de requisito da vaga nao foram avaliados "
                + "(formacao, idioma ou termos fora do dicionario). ";
    }

    private String names(List<RequirementMatch> matches) {
        List<String> names = matches.stream().map(RequirementMatch::name).toList();
        if (names.size() <= MAX_NAMES) {
            return String.join(", ", names);
        }
        return String.join(", ", names.subList(0, MAX_NAMES))
                + " e mais " + (names.size() - MAX_NAMES);
    }

    private String formatScore(double score) {
        if (score == Math.rint(score)) {
            return String.format(Locale.ROOT, "%.0f", score);
        }
        return String.format(Locale.ROOT, "%.1f", score);
    }
}
