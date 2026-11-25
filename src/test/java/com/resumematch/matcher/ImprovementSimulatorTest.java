package com.resumematch.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.resumematch.dictionary.TechnologyDictionary;

class ImprovementSimulatorTest {

    private final SentenceSplitter splitter = new SentenceSplitter();
    private final RequirementMatcher matcher = new RequirementMatcher(new TechnologyDictionary());
    private final ScoreCalculator calculator = new ScoreCalculator();
    private final ImprovementSimulator simulator = new ImprovementSimulator(matcher, calculator);

    private List<RequirementMatch> match(String resume, JobRequirement... requirements) {
        return matcher.matchRequirements(List.of(requirements), resume, splitter.split(resume));
    }

    private ImprovementSimulation simulate(String resume, List<RequirementMatch> matches) {
        return simulator.simulate(matches, resume, splitter.split(resume));
    }

    private static JobRequirement required(String name) {
        return new JobRequirement(name, RequirementPriority.REQUIRED);
    }

    @Test
    void simulacaoNaoAlteraANotaRealNemAsEvidencias() {
        String resume = "Tecnologias: React, Docker.\nDesenvolvi uma API em Java.";
        List<RequirementMatch> matches = new ArrayList<>(match(resume,
                required("React"), required("Docker"), required("Java"), required("Back-end"),
                required("AWS")));
        List<RequirementMatch> before = List.copyOf(matches);
        double scoreBefore = calculator.calculate(matches);

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.suggestions()).isNotEmpty();
        assertThat(matches).isEqualTo(before);
        assertThat(calculator.calculate(matches)).isEqualTo(scoreBefore);
    }

    @Test
    void curriculoSemTermoTecnicoSoTemLacunasReais() {
        String resume = "Trabalhei como atendente de loja e organizei o estoque.";
        List<RequirementMatch> matches = match(resume,
                required("Java"), required("Git"), required("Back-end"), required("Front-end"));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.suggestions()).isEmpty();
        assertThat(simulation.scoreWithAll()).isNull();
        assertThat(simulation.gaps()).extracting(RealGap::requirement)
                .containsExactly("Java", "Git", "Back-end", "Front-end");
    }

    @Test
    void backEndNoCurriculoNaoIndicaJava() {
        String resume = "Desenvolvi o back-end de um sistema de estoque.";
        List<RequirementMatch> matches = match(resume, required("Java"));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.suggestions()).isEmpty();
        assertThat(simulation.gaps()).extracting(RealGap::requirement).containsExactly("Java");
    }

    @Test
    void javaNoCurriculoIndicaBackEnd() {
        String resume = "Desenvolvi uma API em Java.";
        List<RequirementMatch> matches = match(resume, required("Back-end"));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.gaps()).isEmpty();
        ImprovementSuggestion suggestion = simulation.suggestions().getFirst();
        assertThat(suggestion.requirement()).isEqualTo("Back-end");
        assertThat(suggestion.type()).isEqualTo(SuggestionType.INDICATED);
        assertThat(suggestion.indicator()).isEqualTo("Java");
        assertThat(suggestion.evidence()).isEqualTo("Desenvolvi uma API em Java.");
        assertThat(suggestion.points()).isEqualTo(100.0);
    }

    @Test
    void termoNegadoNaoServeDeIndicio() {
        String resume = "Nunca utilizei Java.";
        List<RequirementMatch> matches = match(resume, required("Back-end"));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.suggestions()).isEmpty();
        assertThat(simulation.gaps()).extracting(RealGap::requirement).containsExactly("Back-end");
    }

    @Test
    void reactSoEmListaDaSugestaoComADiferencaEntreAsNotas() {
        String resume = "Tecnologias: React.\nDesenvolvi uma API em Java.";
        List<RequirementMatch> matches = match(resume, required("React"), required("Java"));
        double realScore = calculator.calculate(matches);

        ImprovementSimulation simulation = simulate(resume, matches);

        ImprovementSuggestion suggestion = simulation.suggestions().getFirst();
        assertThat(suggestion.requirement()).isEqualTo("React");
        assertThat(suggestion.type()).isEqualTo(SuggestionType.LIST_ONLY);
        assertThat(suggestion.indicator()).isNull();
        assertThat(suggestion.evidence()).isEqualTo("Tecnologias: React.");
        assertThat(realScore).isEqualTo(75.0);
        assertThat(suggestion.scoreWithChange()).isEqualTo(100.0);
        assertThat(suggestion.points()).isEqualTo(suggestion.scoreWithChange() - realScore);
    }

    @Test
    void notaComTodasAsMudancasAplicaTodasJuntas() {
        String resume = "Tecnologias: React, Docker.\nDesenvolvi uma API em Java.";
        List<RequirementMatch> matches = match(resume,
                required("React"), required("Docker"), required("Java"), required("Back-end"),
                required("AWS"));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.suggestions()).extracting(ImprovementSuggestion::requirement)
                .containsExactly("Back-end", "React", "Docker");
        assertThat(simulation.suggestions()).extracting(ImprovementSuggestion::points)
                .containsExactly(20.0, 10.0, 10.0);
        assertThat(simulation.scoreWithAll()).isEqualTo(80.0);
        assertThat(simulation.gaps()).extracting(RealGap::requirement).containsExactly("AWS");
    }

    /**
     * 7 obrigatorios, 3 so em lista: real 1,5/7 = 21,4; cada mudanca 2/7 = 28,6
     * (+7,2); todas 3/7 = 42,9. A soma das partes da 43,0, e o total continua
     * vindo do ScoreCalculator aplicado a todas as mudancas juntas.
     */
    @Test
    void totalVemDaContaComTodasAsMudancasENaoDaSomaArredondada() {
        String resume = "Tecnologias: React, Docker, Python.";
        List<RequirementMatch> matches = match(resume,
                required("React"), required("Docker"), required("Python"), required("AWS"),
                required("Kubernetes"), required("Terraform"), required("Jenkins"));
        double realScore = calculator.calculate(matches);

        ImprovementSimulation simulation = simulate(resume, matches);

        List<RequirementMatch> allChanges = matches.stream()
                .map(m -> m.status() == MatchStatus.PARTIAL
                        ? new RequirementMatch(m.name(), MatchStatus.MATCH, m.evidence(), m.priority())
                        : m)
                .toList();
        double sumOfParts = realScore + simulation.suggestions().stream()
                .mapToDouble(ImprovementSuggestion::points).sum();
        assertThat(realScore).isEqualTo(21.4);
        assertThat(simulation.suggestions()).extracting(ImprovementSuggestion::points)
                .containsExactly(7.2, 7.2, 7.2);
        assertThat(simulation.scoreWithAll()).isEqualTo(42.9)
                .isEqualTo(calculator.calculate(allChanges));
        assertThat(Math.round(sumOfParts * 10) / 10.0).isEqualTo(43.0);
    }

    @Test
    void diferencialDaMetadeDosPontos() {
        String resume = "Tecnologias: React.\nDesenvolvi uma API em Java.";
        List<RequirementMatch> matches = match(resume,
                new JobRequirement("React", RequirementPriority.PREFERRED), required("Java"));

        ImprovementSimulation simulation = simulate(resume, matches);

        ImprovementSuggestion suggestion = simulation.suggestions().getFirst();
        assertThat(suggestion.priority()).isEqualTo(RequirementPriority.PREFERRED);
        assertThat(suggestion.points()).isEqualTo(16.7);
    }

    @Test
    void conjuntoComParteAusenteViraLacunaEnaoSugestao() {
        String resume = "Conhecimentos: TCP/IP, DNS.";
        List<RequirementMatch> matches = match(resume,
                JobRequirement.composite(List.of("TCP/IP", "DNS", "DHCP"), RequirementPriority.REQUIRED));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(matches.getFirst().status()).isEqualTo(MatchStatus.PARTIAL);
        assertThat(simulation.suggestions()).isEmpty();
        RealGap gap = simulation.gaps().getFirst();
        assertThat(gap.requirement()).isEqualTo("TCP/IP + DNS + DHCP");
        assertThat(gap.missingParts()).containsExactly("DHCP");
    }

    @Test
    void conjuntoComTodasAsPartesEmListaViraSugestao() {
        String resume = "Conhecimentos: TCP/IP, DNS, DHCP.";
        List<RequirementMatch> matches = match(resume,
                JobRequirement.composite(List.of("TCP/IP", "DNS", "DHCP"), RequirementPriority.REQUIRED));

        ImprovementSimulation simulation = simulate(resume, matches);

        assertThat(simulation.gaps()).isEmpty();
        assertThat(simulation.suggestions()).extracting(ImprovementSuggestion::type)
                .containsExactly(SuggestionType.LIST_ONLY);
        assertThat(simulation.scoreWithAll()).isEqualTo(100.0);
    }

    @Test
    void semRequisitoNaoHaSimulacao() {
        assertThat(simulate("Desenvolvi uma API em Java.", List.of()))
                .isEqualTo(ImprovementSimulation.EMPTY);
    }
}
