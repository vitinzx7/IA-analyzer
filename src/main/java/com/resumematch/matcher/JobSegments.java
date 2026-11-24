package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.resumematch.dictionary.TermOccurrence;
import com.resumematch.dictionary.TextNormalizer;

/**
 * Divide a descricao da vaga em trechos e diz de que secao cada um vem.
 *
 * Um titulo reconhecido ("Requisitos", "Diferenciais", "Beneficios") vale ate
 * o proximo titulo. Ele pode ocupar o trecho inteiro ou abrir o trecho com
 * dois-pontos ("Requisitos: Java"). Um rotulo que contem tecnologia do
 * dicionario nunca e titulo: "Conhecimentos em Java" continua sendo requisito.
 *
 * Cada linha e cortada depois de ".", ";", "!" ou "?" seguidos de espaco, para
 * que "Requisitos: Java. Desejavel AWS." trate o diferencial separado.
 */
final class JobSegments {

    record Segment(int start, int end, JobSection section, RequirementPriority priority) {
    }

    private static final int MAX_TITLE_WORDS = 6;
    private static final int MAX_LABEL_WORDS = 4;

    private static final Pattern PREFERRED_CUE = Pattern.compile(
            "\\b(?:diferencia(?:l|is)|desejave(?:l|is)|plus|bonus|nice to have"
                    + "|preferencia(?:l|lmente)|bem[- ]vind[oa]s?|sera considerad[oa]s?)\\b");
    private static final Pattern REQUIRED_CUE = Pattern.compile(
            "\\b(?:obrigatori[oa]s?|imprescindive(?:l|is)|indispensave(?:l|is)"
                    + "|essencia(?:l|is)|mandatori[oa]s?)\\b");

    private static final List<String> PREFERRED_TITLES = List.of(
            "diferenciais", "diferencial", "sera um diferencial", "seria um diferencial",
            "desejavel", "desejaveis", "nice to have", "plus", "bonus", "pontos extras");
    private static final List<String> IGNORED_TITLES = List.of(
            "beneficios", "o que oferecemos", "oferecemos", "informacoes adicionais",
            "local", "localizacao", "carga horaria", "horario", "jornada", "remuneracao",
            "salario", "bolsa", "sobre a empresa", "sobre nos", "quem somos",
            "etapas do processo", "processo seletivo", "modelo de trabalho",
            "regime de contratacao", "benefits", "about us");
    private static final List<String> RESPONSIBILITY_TITLES = List.of(
            "responsabilidades", "atribuicoes", "atividades", "principais atividades",
            "principais responsabilidades", "o que voce vai fazer", "o que voce ira fazer",
            "suas atividades", "descricao das atividades", "funcoes", "dia a dia",
            "responsibilities", "what you will do");
    private static final List<String> REQUIREMENT_TITLES = List.of(
            "requisitos", "requisito", "pre-requisitos", "pre requisitos", "qualificacoes",
            "o que esperamos", "o que buscamos", "o que procuramos", "o que voce precisa",
            "voce precisa ter", "perfil", "competencias", "habilidades", "conhecimentos",
            "exigencias", "requirements", "qualifications", "must have");
    private static final List<String> GENERAL_TITLES = List.of(
            "sobre a vaga", "descricao da vaga", "descricao", "a vaga",
            "sobre a oportunidade", "resumo da vaga", "about the job");

    /**
     * Palavras que podem seguir um titulo sem dois-pontos ("Beneficios
     * oferecidos", "Requisitos e qualificacoes"). Fora delas, a linha e
     * conteudo: "Habilidades de comunicacao" nao muda de secao.
     */
    private static final Set<String> TITLE_WORDS = titleWords(
            "e de da do para a principais adicionais oferecidos obrigatorios obrigatorias"
                    + " desejaveis desejado desejados tecnicos tecnicas necessarios minimos"
                    + " minimas basicos gerais vaga cargo beneficios");

    private JobSegments() {
    }

    /**
     * @param occurrences termos do dicionario no texto da vaga, em ordem
     */
    static List<Segment> read(String jobText, List<TermOccurrence> occurrences) {
        List<Segment> segments = new ArrayList<>();
        JobSection section = JobSection.GENERAL;
        int lineStart = 0;
        while (lineStart < jobText.length()) {
            int lineEnd = jobText.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = jobText.length();
            }
            int pieceStart = lineStart;
            for (int i = lineStart; i < lineEnd; i++) {
                if (".;!?".indexOf(jobText.charAt(i)) >= 0
                        && (i + 1 == lineEnd || Character.isWhitespace(jobText.charAt(i + 1)))) {
                    section = piece(jobText, pieceStart, i + 1, section, occurrences, segments);
                    pieceStart = i + 1;
                }
            }
            section = piece(jobText, pieceStart, lineEnd, section, occurrences, segments);
            lineStart = lineEnd + 1;
        }
        return segments;
    }

    /**
     * Guarda o trecho, se ele nao for so um titulo, e devolve a secao que vale
     * dali em diante.
     */
    private static JobSection piece(String jobText, int start, int end, JobSection current,
            List<TermOccurrence> occurrences, List<Segment> segments) {
        while (start < end && Character.isWhitespace(jobText.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(jobText.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return current;
        }

        String raw = jobText.substring(start, end);
        boolean colonAtEnd = raw.endsWith(":");
        String withoutColon = colonAtEnd ? raw.substring(0, raw.length() - 1) : raw;
        if (withoutColon.indexOf(':') < 0 && !hasTerm(occurrences, start, end)) {
            JobSection title = title(withoutColon, MAX_TITLE_WORDS, !colonAtEnd);
            if (title != null) {
                return title;
            }
        }

        int colon = raw.indexOf(':');
        if (colon > 0 && !hasTerm(occurrences, start, start + colon)) {
            JobSection title = title(raw.substring(0, colon), MAX_LABEL_WORDS, false);
            if (title != null) {
                int contentStart = start + colon + 1;
                while (contentStart < end && Character.isWhitespace(jobText.charAt(contentStart))) {
                    contentStart++;
                }
                if (contentStart < end) {
                    segments.add(segment(jobText, contentStart, end, title));
                }
                return title;
            }
        }

        segments.add(segment(jobText, start, end, current));
        return current;
    }

    private static Segment segment(String jobText, int start, int end, JobSection section) {
        String normalized = TextNormalizer.normalize(jobText.substring(start, end));
        return new Segment(start, end, section, priority(section, normalized));
    }

    private static RequirementPriority priority(JobSection section, String normalized) {
        if (REQUIRED_CUE.matcher(normalized).find()) {
            return RequirementPriority.REQUIRED;
        }
        if (section == JobSection.PREFERRED || PREFERRED_CUE.matcher(normalized).find()) {
            return RequirementPriority.PREFERRED;
        }
        return RequirementPriority.REQUIRED;
    }

    /**
     * Secao anunciada pelo rotulo, ou null quando ele nao e um titulo conhecido.
     *
     * @param strict sem dois-pontos, o rotulo so pode ter o titulo e palavras
     *        de {@link #TITLE_WORDS}; com dois-pontos, basta comecar pelo titulo
     */
    private static JobSection title(String label, int maxWords, boolean strict) {
        String normalized = clean(TextNormalizer.normalize(label));
        if (normalized.isEmpty() || normalized.split("\\s+").length > maxWords) {
            return null;
        }
        if (startsWithAny(normalized, PREFERRED_TITLES, strict)) {
            return JobSection.PREFERRED;
        }
        if (startsWithAny(normalized, IGNORED_TITLES, strict)) {
            return JobSection.IGNORED;
        }
        if (startsWithAny(normalized, RESPONSIBILITY_TITLES, strict)) {
            return JobSection.RESPONSIBILITIES;
        }
        if (startsWithAny(normalized, REQUIREMENT_TITLES, strict)) {
            return PREFERRED_CUE.matcher(normalized).find()
                    ? JobSection.PREFERRED
                    : JobSection.REQUIREMENTS;
        }
        if (startsWithAny(normalized, GENERAL_TITLES, strict)) {
            return JobSection.GENERAL;
        }
        return null;
    }

    /** Tira marcador de lista, emoji e pontuacao das pontas do rotulo. */
    private static String clean(String label) {
        int start = 0;
        int end = label.length();
        while (start < end && !Character.isLetterOrDigit(label.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isLetterOrDigit(label.charAt(end - 1))) {
            end--;
        }
        return label.substring(start, end);
    }

    private static boolean startsWithAny(String label, List<String> titles, boolean strict) {
        for (String title : titles) {
            if (!label.startsWith(title) || (label.length() > title.length()
                    && Character.isLetterOrDigit(label.charAt(title.length())))) {
                continue;
            }
            if (!strict || onlyTitleWords(label.substring(title.length()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean onlyTitleWords(String rest) {
        for (String word : rest.trim().split("[^\\p{L}\\p{N}-]+")) {
            if (!word.isEmpty() && !TITLE_WORDS.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> titleWords(String extra) {
        Set<String> words = new HashSet<>(List.of(extra.split(" ")));
        for (List<String> titles : List.of(PREFERRED_TITLES, IGNORED_TITLES,
                RESPONSIBILITY_TITLES, REQUIREMENT_TITLES, GENERAL_TITLES)) {
            titles.forEach(title -> words.addAll(List.of(title.split(" "))));
        }
        return Set.copyOf(words);
    }

    /** As ocorrencias vem ordenadas pelo inicio, entao a busca e binaria. */
    private static boolean hasTerm(List<TermOccurrence> occurrences, int start, int end) {
        int low = 0;
        int high = occurrences.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (occurrences.get(middle).start() < start) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low < occurrences.size() && occurrences.get(low).start() < end;
    }
}
