package com.resumematch.dictionary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tecnologias conhecidas e seus apelidos.
 *
 * O dicionario e pequeno de proposito: cobre o que aparece no curriculo e nas
 * vagas de teste. A ideia e crescer a partir dos erros observados no log do
 * bloco 5, e nao tentar prever tudo agora.
 */
public class TechnologyDictionary {

    private final Map<String, List<String>> technologies;
    private final List<SearchForm> formsLongestFirst;

    private record SearchForm(String canonical, String normalized) {
    }

    public TechnologyDictionary() {
        this(defaultTechnologies());
    }

    public TechnologyDictionary(Map<String, List<String>> technologies) {
        this.technologies = technologies;
        this.formsLongestFirst = buildForms(technologies);
    }

    public Set<String> knownTechnologies() {
        return new TreeSet<>(technologies.keySet());
    }

    /**
     * Procura no texto todas as tecnologias do dicionario.
     *
     * Casa sempre o termo mais longo primeiro e marca o trecho como consumido,
     * de modo que "java" nao casa dentro de "javascript" e "c" nao casa dentro
     * de "c#". Exige tambem que a borda do termo nao encoste em letra ou
     * digito, quando a propria borda for letra ou digito.
     */
    public List<TermOccurrence> findIn(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        TextNormalizer.Normalized normalized = TextNormalizer.map(text);
        String haystack = normalized.value();
        boolean[] consumed = new boolean[haystack.length()];
        List<TermOccurrence> found = new ArrayList<>();

        for (SearchForm form : formsLongestFirst) {
            int from = 0;
            int start;
            while ((start = haystack.indexOf(form.normalized(), from)) >= 0) {
                int end = start + form.normalized().length();
                if (isFree(consumed, start, end) && hasBoundaries(haystack, start, end)) {
                    markConsumed(consumed, start, end);
                    int originalStart = normalized.originalStart(start);
                    int originalEnd = normalized.originalEnd(end);
                    found.add(new TermOccurrence(form.canonical(),
                            text.substring(originalStart, originalEnd),
                            originalStart, originalEnd));
                }
                from = start + 1;
            }
        }
        found.sort(Comparator.comparingInt(TermOccurrence::start));
        return found;
    }

    private static List<SearchForm> buildForms(Map<String, List<String>> technologies) {
        List<SearchForm> forms = new ArrayList<>();
        technologies.forEach((canonical, aliases) -> {
            forms.add(new SearchForm(canonical, TextNormalizer.normalize(canonical)));
            aliases.forEach(alias ->
                    forms.add(new SearchForm(canonical, TextNormalizer.normalize(alias))));
        });
        forms.sort(Comparator.comparingInt((SearchForm f) -> f.normalized().length()).reversed());
        return forms;
    }

    private static boolean isFree(boolean[] consumed, int start, int end) {
        for (int i = start; i < end; i++) {
            if (consumed[i]) {
                return false;
            }
        }
        return true;
    }

    private static void markConsumed(boolean[] consumed, int start, int end) {
        for (int i = start; i < end; i++) {
            consumed[i] = true;
        }
    }

    /**
     * O termo so vale se nenhum dos lados encostar em letra.
     *
     * A direita, uma sequencia de digitos e aceita quando termina em algo que
     * nao e letra: assim "Java21" e "C++11" sao reconhecidos, mas "Java2EE"
     * nao. A checagem vale inclusive para termos que comecam com pontuacao,
     * senao ".net" seria encontrado dentro do dominio "empresa.net". O ".NET"
     * escrito em "ASP.NET" continua sendo reconhecido, mas pelo alias
     * "asp.net", nao pelo pedaco solto.
     */
    private static boolean hasBoundaries(String text, int start, int end) {
        if (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
            return false;
        }
        if (end >= text.length()) {
            return true;
        }
        char next = text.charAt(end);
        if (Character.isLetter(next)) {
            return false;
        }
        if (Character.isDigit(next)) {
            int afterVersion = end;
            while (afterVersion < text.length() && Character.isDigit(text.charAt(afterVersion))) {
                afterVersion++;
            }
            return afterVersion >= text.length()
                    || !Character.isLetter(text.charAt(afterVersion));
        }
        return true;
    }

    private static Map<String, List<String>> defaultTechnologies() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put("Java", List.of());
        map.put("JavaScript", List.of("js"));
        map.put("TypeScript", List.of("ts"));
        map.put("Python", List.of());
        map.put("VBA", List.of());
        map.put("SQL", List.of());
        map.put("HTML", List.of());
        map.put("CSS", List.of());
        map.put("C#", List.of("csharp", "c sharp"));
        map.put("C++", List.of());
        map.put(".NET", List.of("dotnet", "asp.net"));

        map.put("Spring Boot", List.of("springboot"));
        map.put("Spring Framework", List.of("spring"));
        map.put("React", List.of("reactjs"));
        map.put("Node.js", List.of("nodejs"));
        map.put("API REST", List.of("apis rest", "rest api", "restful", "api restful"));

        map.put("PostgreSQL", List.of("postgres"));
        map.put("MySQL", List.of());
        map.put("H2", List.of());
        map.put("Oracle", List.of());
        map.put("SQL Server", List.of());

        map.put("Azure", List.of("microsoft azure"));
        map.put("Azure Container Apps", List.of());
        map.put("AWS", List.of("amazon web services"));
        map.put("GCP", List.of("google cloud"));

        map.put("Docker", List.of());
        map.put("Docker Compose", List.of("docker-compose"));
        map.put("Kubernetes", List.of("k8s"));
        map.put("Terraform", List.of());
        map.put("Git", List.of());
        map.put("GitHub", List.of());
        map.put("GitLab", List.of());
        map.put("Jenkins", List.of());
        map.put("Maven", List.of());
        map.put("Gradle", List.of());
        map.put("CI/CD", List.of("ci-cd"));

        map.put("JUnit", List.of());
        map.put("Mockito", List.of());
        map.put("MockMvc", List.of());
        map.put("AssertJ", List.of());

        map.put("Windows", List.of());
        map.put("Windows Server", List.of());
        map.put("Linux", List.of());
        map.put("Active Directory", List.of());
        map.put("GPO", List.of());
        map.put("DHCP", List.of());
        map.put("DNS", List.of());
        map.put("TCP/IP", List.of());
        map.put("Proxy", List.of());
        map.put("Redes de Computadores", List.of("redes"));
        map.put("Hardware", List.of());
        map.put("Arquitetura de Computadores", List.of());

        map.put("Microsoft Office", List.of("pacote office", "ms office"));
        map.put("Office 365", List.of("o365", "microsoft 365", "pacote office 365"));
        map.put("Excel", List.of());
        map.put("Power BI", List.of("powerbi"));

        map.put("Help Desk", List.of("helpdesk"));
        map.put("Service Desk", List.of("servicedesk"));
        map.put("ITIL", List.of("itil foundation"));
        map.put("Suporte Técnico", List.of(
                "suporte ao usuario", "suporte aos usuarios", "suporte a usuarios", "suporte de ti"));

        // Chamados, scripts e contas: termos das vagas de suporte e infraestrutura.
        map.put("Jira", List.of("jira software", "jira service management"));
        map.put("Confluence", List.of());
        map.put("Zendesk", List.of());
        map.put("GLPI", List.of());
        map.put("ServiceNow", List.of("service now"));
        map.put("PowerShell", List.of("power shell"));
        map.put("Bash", List.of());
        map.put("Shell Script", List.of("shell scripting", "shell scripts", "script shell", "scripts shell"));
        map.put("Google Workspace", List.of("g suite", "gsuite", "google apps"));
        map.put("Microsoft Teams", List.of("ms teams"));
        map.put("Microsoft Intune", List.of("intune"));
        map.put("Microsoft Exchange", List.of("exchange server", "exchange online"));
        map.put("LDAP", List.of());

        map.put("VPN", List.of("vpns"));
        map.put("VLAN", List.of("vlans"));
        map.put("Firewall", List.of("firewalls"));
        map.put("SSH", List.of());
        map.put("Cabeamento Estruturado", List.of());
        map.put("CCNA", List.of());
        map.put("Ubuntu", List.of());
        map.put("Debian", List.of());
        map.put("Red Hat", List.of("rhel", "red hat enterprise linux"));
        map.put("Kali Linux", List.of());
        map.put("VMware", List.of("vsphere", "esxi"));
        map.put("Hyper-V", List.of("hyperv"));
        map.put("VirtualBox", List.of("virtual box"));
        map.put("Proxmox", List.of());
        map.put("Zabbix", List.of());
        map.put("Nagios", List.of());
        map.put("Grafana", List.of());
        map.put("Prometheus", List.of());

        map.put("Wireshark", List.of());
        map.put("Nmap", List.of());
        map.put("SIEM", List.of("siems"));
        map.put("Splunk", List.of());
        map.put("Wazuh", List.of());
        map.put("Sysmon", List.of());
        map.put("Event Viewer", List.of("visualizador de eventos"));
        map.put("MITRE ATT&CK", List.of("mitre attack"));
        map.put("OWASP", List.of());
        map.put("ISO 27001", List.of("iso/iec 27001"));
        map.put("LGPD", List.of("lei geral de protecao de dados"));

        map.put("RPA", List.of("robotic process automation"));
        map.put("UiPath", List.of());
        map.put("Automation Anywhere", List.of());
        map.put("Blue Prism", List.of());

        return map;
    }
}
