package com.resumematch.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.List;

import org.junit.jupiter.api.Test;

class TechnologyDictionaryTest {

    private final TechnologyDictionary dictionary = new TechnologyDictionary();

    private List<String> technologiesIn(String text) {
        return dictionary.findIn(text).stream().map(TermOccurrence::technology).toList();
    }

    @Test
    void naoEncontraJavaDentroDeJavaScript() {
        assertThat(technologiesIn("Experiencia com JavaScript e React"))
                .containsExactly("JavaScript", "React")
                .doesNotContain("Java");
    }

    @Test
    void encontraJavaQuandoEstaSozinho() {
        assertThat(technologiesIn("Desenvolvi APIs com Java 21")).contains("Java");
    }

    @Test
    void resolveAliasParaONomeCanonico() {
        assertThat(technologiesIn("Banco Postgres, front em js e deploy em k8s"))
                .containsExactlyInAnyOrder("PostgreSQL", "JavaScript", "Kubernetes");
    }

    @Test
    void casaOTermoMaisLongoPrimeiro() {
        assertThat(technologiesIn("Usei Docker Compose no projeto"))
                .containsExactly("Docker Compose")
                .doesNotContain("Docker");
    }

    @Test
    void naoEncontraCDentroDeCSustenido() {
        assertThat(technologiesIn("Projetos em C# e .NET"))
                .containsExactlyInAnyOrder("C#", ".NET");
    }

    @Test
    void encontraTermoAcentuadoDoTextoOriginal() {
        assertThat(technologiesIn("Conhecimento em redes de computadores"))
                .contains("Redes de Computadores");
    }

    @Test
    void guardaOTrechoOriginalComAcentoECaixa() {
        List<TermOccurrence> found = dictionary.findIn("Trabalhei com Docker e PostgreSQL");

        assertThat(found).extracting(TermOccurrence::matchedText)
                .containsExactly("Docker", "PostgreSQL");
    }

    @Test
    void indicesApontamParaOTextoOriginal() {
        String text = "Experiência em manutenção com Docker";

        TermOccurrence docker = dictionary.findIn(text).getFirst();

        assertThat(text.substring(docker.start(), docker.end())).isEqualTo("Docker");
    }

    @Test
    void ignoraTextoVazio() {
        assertThat(dictionary.findIn("")).isEmpty();
        assertThat(dictionary.findIn(null)).isEmpty();
    }

    @Test
    void naoRepeteOMesmoTrechoEmDoisTermos() {
        assertThat(technologiesIn("Azure Container Apps")).containsExactly("Azure Container Apps");
    }

    @Test
    void pacoteOffice365NaoViraMicrosoftOffice() {
        assertThat(technologiesIn("conhecimento do pacote Office 365"))
                .containsExactly("Office 365");
    }

    @Test
    void pacoteOfficeSozinhoContinuaSendoMicrosoftOffice() {
        assertThat(technologiesIn("Pacote Office / ferramentas corporativas"))
                .containsExactly("Microsoft Office");
    }

    @Test
    void dominioTerminadoEmNetNaoViraTecnologia() {
        assertThat(technologiesIn("Portfolio em https://empresa.net/vagas")).isEmpty();
        assertThat(technologiesIn("Acesse www.minhaempresa.net hoje")).isEmpty();
    }

    @Test
    void continuaEncontrandoDotNetEscritoComoTecnologia() {
        assertThat(technologiesIn("Desenvolvimento com .NET")).containsExactly(".NET");
        assertThat(technologiesIn("Experiencia em ASP.NET Core")).containsExactly(".NET");
    }

    @Test
    void recortaEvidenciaCorretaEmTextoDecomposto() {
        String decomposto = Normalizer.normalize(
                "Manutenção de containers Docker", Normalizer.Form.NFD);

        List<TermOccurrence> found = dictionary.findIn(decomposto);

        assertThat(found).singleElement().satisfies(o -> {
            assertThat(o.technology()).isEqualTo("Docker");
            assertThat(o.matchedText()).isEqualTo("Docker");
            assertThat(decomposto.substring(o.start(), o.end())).isEqualTo("Docker");
        });
    }

    @Test
    void recortaEvidenciaCorretaQuandoOAcentoVemAntesDoTermo() {
        String texto = "Configuração Docker e manutenção PostgreSQL";

        assertThat(dictionary.findIn(texto))
                .extracting(o -> texto.substring(o.start(), o.end()))
                .containsExactly("Docker", "PostgreSQL");
    }

    @Test
    void reconheceVersaoColadaNoNome() {
        assertThat(technologiesIn("Projetos em C++11, Java21, Python3 e Windows11"))
                .containsExactlyInAnyOrder("C++", "Java", "Python", "Windows");
    }

    @Test
    void naoReconheceTermoQuandoVersaoColadaContinuaComLetra() {
        assertThat(technologiesIn("Trabalhei com Java2EE")).isEmpty();
    }

    @Test
    void reconheceFerramentasDeSuporte() {
        assertThat(technologiesIn(
                "Chamados no Jira, Zendesk e GLPI; scripts em PowerShell e Bash; contas no G Suite."))
                .containsExactly("Jira", "Zendesk", "GLPI", "PowerShell", "Bash", "Google Workspace");
        assertThat(technologiesIn("Prestei suporte aos usuários e configurei VPNs e VLANs."))
                .containsExactly("Suporte Técnico", "VPN", "VLAN");
    }

    @Test
    void reconheceTermosDeSeguranca() {
        assertThat(technologiesIn("Analisei logs do Sysmon no Splunk e no Wazuh com MITRE ATT&CK."))
                .containsExactly("Sysmon", "Splunk", "Wazuh", "MITRE ATT&CK");
        assertThat(technologiesIn("Varredura com Nmap e captura no Wireshark."))
                .containsExactly("Nmap", "Wireshark");
    }

    @Test
    void reconheceBackEndEFrontEndComOuSemHifen() {
        assertThat(technologiesIn("backend, back end, Back-end; frontend, front end, Front-End"))
                .containsExactly("Back-end", "Back-end", "Back-end", "Front-end", "Front-end", "Front-end");
    }

    @Test
    void distribuicaoLinuxEhUmTermoSo() {
        assertThat(technologiesIn("Laboratorio com Kali Linux e Ubuntu")).containsExactly("Kali Linux", "Ubuntu");
    }
}
