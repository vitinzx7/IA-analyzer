package com.resumematch.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.resumematch.dictionary.TextNormalizer;

/**
 * Acha os titulos de secao do curriculo ("PROJETOS", "Experiencia
 * profissional") e diz em que secao cada posicao do texto esta.
 *
 * Titulo e uma linha curta, de ate quatro palavras e sem ponto, formada por um
 * titulo conhecido e, se houver, palavras de qualificacao ("Competencias
 * tecnicas", "Cursos complementares"). Linhas como "Ferramentas: Docker" nao
 * sao titulo. O texto antes do primeiro titulo e o cabecalho. Sem nenhum
 * titulo, tudo fica em OTHER e a secao nao muda a escolha da evidencia.
 */
final class ResumeSections {

    private static final int MAX_WORDS = 4;

    private static final Map<String, ResumeSection> TITLES = Map.ofEntries(
            Map.entry("resumo", ResumeSection.SUMMARY),
            Map.entry("perfil", ResumeSection.SUMMARY),
            Map.entry("objetivo", ResumeSection.SUMMARY),
            Map.entry("objetivos", ResumeSection.SUMMARY),
            Map.entry("sobre mim", ResumeSection.SUMMARY),
            Map.entry("apresentacao", ResumeSection.SUMMARY),
            Map.entry("summary", ResumeSection.SUMMARY),
            Map.entry("competencias", ResumeSection.SKILLS),
            Map.entry("habilidades", ResumeSection.SKILLS),
            Map.entry("conhecimentos", ResumeSection.SKILLS),
            Map.entry("tecnologias", ResumeSection.SKILLS),
            Map.entry("ferramentas", ResumeSection.SKILLS),
            Map.entry("skills", ResumeSection.SKILLS),
            Map.entry("hard skills", ResumeSection.SKILLS),
            Map.entry("experiencia", ResumeSection.EXPERIENCE),
            Map.entry("experiencias", ResumeSection.EXPERIENCE),
            Map.entry("historico profissional", ResumeSection.EXPERIENCE),
            Map.entry("atuacao profissional", ResumeSection.EXPERIENCE),
            Map.entry("experience", ResumeSection.EXPERIENCE),
            Map.entry("projetos", ResumeSection.PROJECTS),
            Map.entry("portfolio", ResumeSection.PROJECTS),
            Map.entry("projects", ResumeSection.PROJECTS),
            Map.entry("formacao", ResumeSection.EDUCATION),
            Map.entry("educacao", ResumeSection.EDUCATION),
            Map.entry("escolaridade", ResumeSection.EDUCATION),
            Map.entry("education", ResumeSection.EDUCATION),
            Map.entry("cursos", ResumeSection.COURSES),
            Map.entry("certificacoes", ResumeSection.COURSES),
            Map.entry("certificados", ResumeSection.COURSES),
            Map.entry("certifications", ResumeSection.COURSES),
            Map.entry("idiomas", ResumeSection.LANGUAGES),
            Map.entry("idioma", ResumeSection.LANGUAGES),
            Map.entry("languages", ResumeSection.LANGUAGES));

    private static final Set<String> QUALIFIERS = Set.of(
            "tecnicas", "tecnicos", "profissional", "profissionais", "academica", "academicos",
            "complementares", "pessoais", "principais", "relevantes", "adicionais", "e", "de",
            "extracurriculares", "certificacoes", "cursos", "conhecimentos", "habilidades",
            "idiomas", "tecnologias", "ferramentas", "skills", "work", "professional");

    private record Heading(int start, ResumeSection section) {
    }

    private final List<Heading> headings;

    private ResumeSections(List<Heading> headings) {
        this.headings = headings;
    }

    static ResumeSections read(String text) {
        List<Heading> headings = new ArrayList<>();
        if (text == null) {
            return new ResumeSections(headings);
        }
        int lineStart = 0;
        while (lineStart < text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            ResumeSection section = title(text.substring(lineStart, lineEnd));
            if (section != null) {
                headings.add(new Heading(lineStart, section));
            }
            lineStart = lineEnd + 1;
        }
        return new ResumeSections(headings);
    }

    ResumeSection sectionAt(int position) {
        if (headings.isEmpty()) {
            return ResumeSection.OTHER;
        }
        ResumeSection section = ResumeSection.HEADER;
        for (Heading heading : headings) {
            if (heading.start() > position) {
                break;
            }
            section = heading.section();
        }
        return section;
    }

    private static ResumeSection title(String line) {
        String clean = line.strip();
        if (clean.endsWith(":")) {
            clean = clean.substring(0, clean.length() - 1).strip();
        }
        if (clean.isEmpty() || clean.length() > 40 || clean.contains(".") || clean.contains(":")) {
            return null;
        }
        String normalized = TextNormalizer.normalize(clean);
        String[] words = normalized.split("\\s+");
        if (words.length > MAX_WORDS) {
            return null;
        }
        for (int take = Math.min(2, words.length); take >= 1; take--) {
            String key = String.join(" ", List.of(words).subList(0, take));
            ResumeSection section = TITLES.get(key);
            if (section != null && List.of(words).subList(take, words.length).stream()
                    .allMatch(QUALIFIERS::contains)) {
                return section;
            }
        }
        return null;
    }
}
