# Decisões travadas

Decisões que já foram tomadas e não devem ser rediscutidas sem evidência nova.
Cada relatório de bloco aponta para cá em vez de repetir esta lista.

Última atualização: interface com PDF ou texto e score nulo.

---

## D1 — Regra de classificação MATCH / PARTIAL / MISSING

Implementado no Bloco 3 (`SentenceSplitter`, `ActionVerbs`,
`RequirementMatcher`).

A unidade de análise é a **frase**. Não existe critério de "proximidade" nem
janela de N palavras, por não serem implementáveis sem virar um número
arbitrário.

```
MATCH   = o termo ocorre em ao menos uma frase que contém pelo menos um
          verbo da lista
PARTIAL = o termo ocorre, mas só em frases sem nenhum verbo da lista
MISSING = o termo não ocorre, ou só ocorre negado
```

Lista de verbos, restrita à **1ª pessoa do passado**: desenvolvi, implementei,
criei, publiquei, realizei, utilizei, configurei, apoiei, mantive, escrevi.

A restrição à 1ª pessoa do passado é o que faz a regra funcionar. Ela exclui os
substantivos das listas de competência (instalação, configuração, diagnóstico)
e exclui os verbos de posse do resumo. Ver o achado O3 em
[OBSERVACOES-TEXTO-REAL](#) mais abaixo neste arquivo.

**Não há critério de contagem mínima de palavras.**

**Negação do verbo.** Um verbo da lista não conta quando vem logo depois de
`não`, `nunca`, `jamais` ou `sem`, na mesma oração (a oração começa depois da
última `,` ou `;`). "Nunca utilizei AWS." não é MATCH. "Sem experiência
prévia, desenvolvi uma API em Java." continua MATCH, porque a negação está em
outra oração.

**Negação do termo.** Uma ocorrência negada não é evidência nenhuma: não conta
nem para MATCH nem para PARTIAL. Duas formas contam, sempre na mesma oração:

```
negação logo antes do termo, artigo opcional
  "Desenvolvi sistemas sem Docker."      -> Docker MISSING
  "Desenvolvi sistemas sem o Docker."    -> Docker MISSING
oração com verbo da lista, todos negados
  "Nunca utilizei AWS."                  -> AWS MISSING
  "Nunca utilizei AWS, mas configurei Azure." -> AWS MISSING, Azure MATCH
```

Antes, "Nunca utilizei AWS." era PARTIAL e somava meio ponto a quem declarou
não ter a tecnologia. "Sem" que nega outra coisa não atrapalha: em
"Configurei pipelines sem downtime com Jenkins." o Jenkins continua MATCH.
Coerente com a regra de não usar janela de N palavras, "sem experiência em
Java" não é reconhecido como negação (ver L14).

### D1 — nota de proveniência

Duas rodadas de review afirmaram que a seção 4.3 do plano define
`MATCH = frase com >= 6 palavras E pelo menos um verbo da lista`, e pediram
que o histórico atribuísse essa origem ao documento.

Verificação executada nos dois arquivos do plano presentes na máquina:

```
$ md5sum "PLANO-RESUME-MATCH-POC.md" "PLANO-RESUME-MATCH-POC (1).md"
001606a9007e084c68bff99bf19fa4d3   (idêntico nos dois arquivos)

$ grep -n "palavras" <cada arquivo>
(nenhuma ocorrência em todo o documento)
```

Texto integral da seção 4.3:

```
1x numa frase com contexto (verbo, ação)  → MATCH
1x ou mais só em lista solta              → PARTIAL
não aparece                               → MISSING

"Qualidade decide, frequência só reforça."

"Tecnologias: Java, Docker, Git"           → PARTIAL
"Coloquei a API em container com Docker"   → MATCH
```

O plano não define contagem de palavras e sequer define uma lista de verbos.
Ambos foram propostos em conversa. A decisão de **não** usar contagem
permanece, para não criar um segundo parâmetro de calibragem sem evidência de
que seja necessário. O que foi recusado foi apenas a reatribuição da origem.

Ressalva: se existir versão mais recente do plano, ela não está na máquina.
Reabrir o ponto caso seja apresentada.

---

## D2 — Tratamento de quebra de linha

Implementado no Bloco 3 (`SentenceSplitter`).

**Passada 1, remontar parágrafos.** Junta a linha com a seguinte usando um
espaço, exceto quando a linha termina em `.`, `!` ou `?`, ou a próxima começa
com marcador de bullet, ou a próxima é cabeçalho de seção (curta e toda em
maiúsculas), ou a linha está vazia.

Decidido **não** usar `:` como fim de parágrafo. O texto real mostrou
`Conclusão prevista:` quebrando antes de `2028`.

**Linha de contato nunca é juntada** com a anterior nem com a seguinte.
Contato é a linha que tem um e-mail de verdade (`nome@dominio.tld`), um
telefone com DDD ou um link (`http`, `www`, `github.com`, `linkedin.com`). Um
`@` solto, como `@RestController`, ou um `|` não fazem a linha virar contato:
as duas coisas aparecem em frases de projeto que quebram de linha.

**Uma linha em branco sozinha não corta a frase** quando a linha seguinte
começa com minúscula. Alguns leitores de PDF põem uma linha vazia no meio da
frase ("containers Docker" / "" / "no Azure Container Apps."), e o verbo
ficava longe da tecnologia. Duas linhas em branco, ou a seguinte com
maiúscula, continuam separando parágrafos.

**Linha com cara de nome não se junta à seguinte**: duas a seis palavras, só
letras, todas com inicial maiúscula (fora "de", "da", "do", "dos", "das" e
"e"). Sem isso, "Victor Douglas Sant'Anna Dideus" grudava em "Estudante de TI
| ... Redes" e o nome virava evidência de "Redes de Computadores".

**Passada 2, separar frases** sobre o texto já remontado, por `.`, `!`, `?` e
`;`, tratando cada item de bullet como frase própria mesmo sem pontuação final.

O ponto não separa frase quando faz parte de nome ou endereço:

```
ponto colado entre letra/dígito e minúscula   Node.js, github.com/x/y, fulano.dev
trecho curto em maiúsculas, prefixo maiúsculo ASP.NET, VB.NET, ADO.NET
ponto antes de nome curto                     .NET
```

"Usei Java.SQL é essencial" separa em "Usei Java." e "SQL é essencial": o
prefixo "Java" não é todo maiúsculo.

---

## D3 — Dicionário

Implementado no Bloco 2.

Começar pequeno, cobrindo apenas currículo e vagas de teste, e crescer a partir
dos erros observados no CSV do Bloco 5. Casar sempre o termo mais longo
primeiro, com limite de palavra. Preservar `C#`, `C++`, `.NET` e `Node.js`, que
morrem se a normalização remover pontuação.

Precisa cobrir termos de suporte, infraestrutura, office e RPA, e não apenas
stack de desenvolvimento — as duas vagas reais são de suporte.

Ampliação para vagas de suporte, infraestrutura e segurança, a partir dos
nomes que a lista "não avaliado" (D15) mostrou numa vaga real de estágio em
infraestrutura e de termos comuns nesse tipo de vaga:

```
chamados      Jira, Confluence, Zendesk, GLPI, ServiceNow, Suporte Técnico
scripts       PowerShell, Bash, Shell Script
contas        Google Workspace (G Suite), Microsoft Teams, Intune, Exchange, LDAP
redes         VPN, VLAN, Firewall, SSH, Cabeamento Estruturado, CCNA
sistemas      Ubuntu, Debian, Red Hat, Kali Linux
virtualização VMware, Hyper-V, VirtualBox, Proxmox
monitoramento Zabbix, Nagios, Grafana, Prometheus
segurança     Wireshark, Nmap, SIEM, Splunk, Wazuh, Sysmon, Event Viewer,
              MITRE ATT&CK, OWASP, ISO 27001, LGPD
```

A lista foi escrita à mão, não importada. A consulta à API do ESCO em
português não trouxe conceito para Jira, DHCP, Active Directory nem
PowerShell, e os rótulos estavam em português de Portugal; importar uma
taxonomia geral traria termos comuns que viram falso positivo.

---

## D4 — Commits

Um commit por bloco por padrão, com commits extras quando houver um checkpoint
que mereça marcação própria. Sem linha de co-autoria de IA. E-mail real do
usuário em todos os commits.

---

## D5 — Arquivo de instruções fora do versionamento

O `CLAUDE.md` fica no `.gitignore`, por decisão do usuário, para não ficar
visível aos revisores e não influenciar o review.

---

## D6 — Equivalências unidirecionais entre tecnologias

Implementado no Bloco 3 (`TechnologyEquivalence`).

Uma equivalência vale só na direção declarada: o termo do currículo satisfaz o
requisito da vaga, mas o contrário não. Regras atuais:

```
Spring Boot             satisfaz  Spring Framework
Office 365              satisfaz  Microsoft Office
PostgreSQL              satisfaz  SQL
MySQL                   satisfaz  SQL
SQL Server              satisfaz  SQL
Oracle                  satisfaz  SQL
H2                      satisfaz  SQL
Azure Container Apps    satisfaz  Azure
Docker Compose          satisfaz  Docker
Kali Linux, Ubuntu,
Debian, Red Hat         satisfazem Linux
Windows Server          satisfaz  Windows
Bash                    satisfaz  Shell Script
```

As distribuições precisam da regra porque o casamento mais longo consome
"Kali Linux" inteiro, e o "Linux" de dentro deixaria de contar.

As duas ultimas linhas cobrem termos especificos que, sem a equivalencia, eram
consumidos pelo casamento mais longo e deixavam de satisfazer o termo generico
da vaga. A direcao continua unica: `Azure` nao satisfaz `Azure Container Apps`
e `Docker` nao satisfaz `Docker Compose`.

`Spring Framework` **não** satisfaz `Spring Boot`. A assimetria evita apagar a
distinção de quem pediu a ferramenta específica. A equivalência só promove o
requisito quando a evidência já existe no currículo; ela não cria evidência.

---

## D7 — Fórmula do score

Implementado no Bloco 4 (`ScoreCalculator`).

```
MATCH = 1,0 · PARTIAL = 0,5 · MISSING = 0,0
peso: obrigatório = 1,0 · diferencial = 0,5 (ver D13)
score = Σ (peso × pontos) / Σ peso × 100
```

Arredondado para uma casa decimal. O denominador é a soma dos pesos dos
requisitos **reconhecidos**, não o número de requisitos escritos na vaga (ver
L2). Sem diferencial na vaga, a fórmula é igual à original: todos pesam 1.

Quando nenhum requisito é reconhecido, a API devolve `"score": null`, e não
`0`: não existe nota a calcular, e o resumo já diz isso. O `ScoreCalculator`
continua devolvendo `0` para lista vazia; quem decide o `null` é o
`AnalysisService`.

---

## D8 — Resumo local, sem IA obrigatória

Implementado no Bloco 4 (`LocalSummaryGenerator`).

O resumo é montado por template, em português, sem chamada externa. Sugestões
são sempre condicionais e nunca afirmam que a pessoa possui uma experiência. O
Gemini permanece opcional e fora do escopo desta POC: o sistema funciona
normalmente sem chave e sem rede.

---

## D9 — CSV local e desligável

Implementado no Bloco 5 (`AnalysisCsvLogger`).

Uma linha por requisito, cabeçalho `data,requisito,status,evidencia`, arquivo
`analises.csv` na raiz de execução. Caminho e ativação vêm de
`resumematch.csv.path` e `resumematch.csv.enabled`. Falha de escrita é logada e
não derruba a análise. O arquivo está no `.gitignore` porque contém trechos do
currículo.

A evidência vem do PDF, ou seja, de quem escreveu o currículo. Um campo que
comece com `=`, `+`, `-` ou `@` recebe um apóstrofo na frente, para que uma
planilha não o execute como fórmula (CSV injection).

---

## D10 — API e interface

Implementado nos Blocos 6 e 7.

- `POST /api/analyze` recebe `multipart/form-data` com `resume` (PDF) e `job`
  (texto).
- `POST /api/analyze/text` recebe JSON `{ "resume": "...", "job": "..." }`.
- `GET /api/health` responde `{"status":"ok"}`.
- A interface está em `/` e usa apenas HTML, CSS e JavaScript, sem framework
  e sem recurso externo (fonte, CDN ou ícone carregado de fora).

A interface aceita o currículo em PDF (seleção ou arrastar) ou como texto
colado, que usa `/api/analyze/text`. O tamanho do PDF e dos textos é conferido
no navegador antes do envio, além da validação no servidor. Score `null` é
mostrado como "Sem nota". O termo é destacado na evidência montando nós de
texto, nunca HTML a partir do conteúdo do currículo. Apelidos (`k8s`) e
equivalências (`Spring Boot` para `Spring`) não recebem destaque.

O upload é validado pelo conteúdo (`%PDF-` nos primeiros bytes), não pela
extensão. Limites: 10 MB por PDF, 200 mil caracteres de currículo e 50 mil
caracteres de vaga. Erros usam `ProblemDetail` (RFC 9457).

---

## D11 — Requisitos de alternativas na vaga

Implementado em `JobRequirementExtractor`, `TechnologyFamilies` e
`RequirementAlternatives`.

Problema real: "Azure, AWS ou GCP" virava três requisitos, e quem tinha só
Azure recebia dois MISSING falsos.

Regra: na vaga, uma lista separada por vírgulas que termina em "ou" (ou
"e/ou"), ou termos ligados por "/", vira **um** requisito de alternativas
somente no trecho final da lista em que todos os termos são da mesma família.
O resto continua separado.

```
"Azure, AWS ou GCP"                     -> [Azure / AWS / GCP]
"Java, Docker, Azure, AWS ou GCP"       -> [Java] [Docker] [Azure / AWS / GCP]
"Java, Spring Boot, Docker, Git ou GitHub" -> 5 requisitos separados
"Windows/Linux"                         -> [Windows] [Linux]
```

Famílias iniciais, pequenas de propósito:

```
nuvem             Azure, AWS, GCP
banco relacional  PostgreSQL, MySQL, SQL Server, Oracle
hospedagem de Git GitHub, GitLab
build Java        Maven, Gradle
chamados          Jira, Zendesk, GLPI, ServiceNow
shell             PowerShell, Bash, Shell Script
suíte de escritório Google Workspace, Office 365
virtualização     VMware, Hyper-V, VirtualBox, Proxmox
monitoramento     Zabbix, Nagios, Prometheus, Grafana
SIEM              Splunk, Wazuh
```

`Windows/Linux` não agrupa: em vaga de suporte a barra costuma pedir os dois.
Entre parênteses, "(Windows/Linux)" vira um conjunto (ver D14).

O grupo é satisfeito por qualquer opção que tenha evidência, segue a mesma
regra D1 e vale **1** requisito no score. Uma tecnologia que já está num grupo
não entra de novo como requisito sozinha, em qualquer ordem do texto.

**Diferença em relação à primeira proposta.** A proposta original mandava
separar todos os termos quando a lista inteira não fosse da mesma família.
Isso deixaria "Java, Docker, Azure, AWS ou GCP" com os mesmos dois MISSING
falsos. Por isso o agrupamento olha só o trecho final da lista.

---

## D12 — Evidência sem e-mail e telefone

Implementado em `PersonalData` e `RequirementMatcher`.

Antes de a evidência sair do matcher, e portanto antes de chegar à tela e ao
CSV, e-mail vira `[email]` e telefone vira `[telefone]`.

O padrão de telefone exige DDD. Sem essa exigência, "2026-2028" teria o mesmo
formato de um telefone de oito dígitos. Verificado em teste que ano, versão
("Java 21"), período ("01/2024 - 12/2025") e IP não são mascarados.

---

## D13 — Seções da vaga e peso do diferencial

Implementado em `JobSegments`, `JobRequirementExtractor`,
`RequirementPriority` e `ScoreCalculator`.

Problema real: numa vaga de estágio em infraestrutura, obrigatório,
responsabilidade e diferencial pesavam igual, e um termo citado em benefícios
ou no endereço podia virar requisito.

Regra: a vaga é lida em trechos (a linha cortada depois de ".", ";", "!" ou
"?" seguidos de espaço). Um título conhecido muda a seção até o próximo
título:

```
Requisitos, Qualificações, Perfil, Conhecimentos ...     -> REQUIREMENTS
Responsabilidades, Atribuições, Atividades ...           -> RESPONSIBILITIES
Diferenciais, Desejável, Será um diferencial, Plus ...   -> PREFERRED
Benefícios, Informações adicionais, Local, Horário ...   -> IGNORED
Sobre a vaga, Descrição ...                              -> GENERAL
```

O título pode ocupar a linha ("Diferenciais") ou abrir a linha com
dois-pontos ("Requisitos: Java"). Sem dois-pontos, a linha só é título se tiver
o título e palavras de qualificação ("Benefícios oferecidos", "Requisitos e
qualificações"); assim "Habilidades de comunicação" não muda de seção. Um
rótulo com termo do dicionário nunca é título: "Conhecimentos em Java" é
requisito.

Prioridade do trecho:

```
"imprescindível", "obrigatório", "indispensável", "essencial"   -> REQUIRED
seção PREFERRED, ou "desejável", "diferencial", "plus", "bônus"  -> PREFERRED
o resto (inclusive GENERAL e RESPONSIBILITIES)                   -> REQUIRED
```

O que está em IGNORED não vira requisito. Um requisito citado em mais de um
trecho fica com a maior prioridade.

Peso: obrigatório 1,0, diferencial 0,5. **Os dois números são ponto de
partida, não medida.** As propostas consideradas sugeriam pesos diferentes
entre si (3/2/1/0,5; 1/0,8/0,4; 1/0,5), o que mostra que o peso precisa ser
calibrado com decisões marcadas como certas ou erradas, não escolhido por
opinião. Responsabilidade pesa como obrigatório porque costuma
repetir o requisito com outras palavras.

---

## D14 — Termos entre parênteses viram um requisito

Implementado em `JobRequirementExtractor`, `JobRequirement` e
`RequirementMatcher`.

Problema real: "noções de redes (TCP/IP, DNS, DHCP)" virava três requisitos.
Junto com "Redes de Computadores", uma linha da vaga chegava a 40% do score.

Regra: dois ou mais termos diferentes dentro do mesmo parêntese viram um
requisito só.

```
"(TCP/IP, DNS, DHCP)"                         -> conjunto TCP/IP + DNS + DHCP
"(Windows/Linux)"                             -> conjunto Windows + Linux
"(AWS, Azure ou GCP)"                         -> alternativas (regra D11)
"(ex.: UiPath, Blue Prism ou similares)"      -> alternativas: UiPath / Blue Prism
"(Python, VBA, SQL, etc.)"                    -> alternativas: Python / VBA / SQL
"RPA (Robotic Process Automation)"            -> RPA, nada muda
```

"ex.", "como", "etc.", "similares", "outros" ou "afins" no parêntese indicam
lista de exemplos: qualquer um basta. Termos todos da mesma família (D11)
também viram alternativas, mesmo sem "ou": "ferramentas de chamados (Jira,
Zendesk, GLPI)" pede uma ferramenta de chamado, não as três. Fora desses
casos, a lista é um conjunto, avaliado parte por parte:

```
todas as partes MATCH       -> MATCH
nenhuma parte aparece       -> MISSING
o resto                     -> PARTIAL
```

O conjunto continua no modelo de D1 e D7 (1 / 0,5 / 0): não há ponto
fracionado. A evidência é a da primeira parte com MATCH, ou da primeira com
PARTIAL. A API manda a decisão de cada parte em `components`.

**Diferença em relação às propostas da pesquisa.** Uma proposta dava 1/N por
parte; outra dava MATCH com uma parte só. A primeira foge do modelo 1 / 0,5 /
0; a segunda daria nota cheia a quem só sabe DNS numa vaga que pede TCP/IP,
DNS e DHCP. Também foi descartado transformar toda lista com "e" num
conjunto: "Java, Spring Boot, Docker e Kubernetes" viraria um requisito só, o
mesmo erro que a D11 corrigiu.

---

## D15 — O que não foi avaliado aparece na tela

Implementado em `JobRequirementExtractor`, `UnknownTerms`, `JobExtraction` e
`app.js`.

Problema real: formação, idioma e ferramentas fora do dicionário (Jira, GLPI,
PowerShell) sumiam sem aviso, e o número parecia cobrir a vaga inteira.

Regra: nos trechos de requisitos, responsabilidades e diferenciais:

- trecho que começa por formação ("Cursando", "Ensino superior", "Graduação",
  "Formação"...) vai para a lista com motivo `EDUCATION` e **não gera
  requisito técnico**: em "Cursando ... Redes de Computadores" o termo é nome
  de curso;
- trecho sem termo do dicionário vai para a lista com motivo `LANGUAGE` (se
  cita idioma) ou `NO_KNOWN_TERM`;
- palavra com maiúscula no meio da frase que o dicionário não reconhece vai
  para `unknownTerms` ("Jira", "GLPI", "Google Workspace"). A primeira palavra
  do trecho só conta se tiver maiúscula além da inicial ("PowerShell" conta,
  "Apoiar" não).

A apresentação da empresa e os benefícios não entram na lista. Nada disso
entra no score; a lista existe para que a pessoa saiba o que conferir sozinha
e para mostrar onde o dicionário precisa crescer.

---

## D16 — Medir o acerto com marcações de certo ou errado

Implementado em `Feedback`, `FeedbackStore`, `FeedbackMetrics`,
`FeedbackController` e `app.js`.

Problema real: o score foi comparado com notas dadas por opinião, que variavam
de 50 a 80 para o mesmo currículo. Não existe conjunto público em português de
pares currículo × vaga com rótulo humano, e o plano já pedia marcar à mão os
falsos positivos e falsos negativos (5.2).

Regra: cada decisão do resultado pode ser marcada como certa, ou como "devia
ser" outro status. A marcação vai para `feedback.csv` com data, identificador
da análise (gerado pela tela), nome do requisito, status do programa e status
correto. Nenhum trecho do currículo entra no arquivo. A mesma marcação repetida
na mesma análise vale uma vez só, a última.

Métricas por status, arredondadas para uma casa:

```
precisão  = marcações certas com status X / marcações em que o programa disse X
revocação = marcações certas com status X / marcações em que o certo era X
acerto    = marcações certas / total
```

Proporção sem nenhum caso fica `null`, não `0`. Com o registro desligado, a
API recusa a marcação com uma mensagem em vez de fingir que gravou.

É isso que permite calibrar os pesos da D13 e medir se uma mudança de regra
melhora ou piora: marcar um conjunto de análises, mudar a regra, analisar de
novo e comparar.

---

## D17 — Evidência escolhida pela seção do currículo

Implementado em `ResumeSections`, `ResumeSection` e `RequirementMatcher`.

Problema real: a evidência de "Redes de Computadores" era o nome da pessoa
colado no título do currículo, porque era a primeira menção do texto.

Regra: os títulos de seção do currículo ("RESUMO", "COMPETÊNCIAS TÉCNICAS",
"PROJETOS", "EXPERIÊNCIA PROFISSIONAL", "FORMAÇÃO ACADÊMICA", "CURSOS
COMPLEMENTARES", "IDIOMAS") dividem o texto. O que vem antes do primeiro
título é o cabeçalho. Entre as menções válidas de um requisito:

```
1. frase com verbo de ação vence frase sem verbo (o status segue a D1)
2. entre iguais, vence a seção mais forte:
   experiência, projetos  >  cursos, formação  >  outra  >  competências,
   idiomas  >  resumo  >  cabeçalho
3. no empate, a primeira
```

O status não muda por causa da seção: só a evidência mostrada. A API manda a
seção em `evidenceSection`. Currículo sem nenhum título reconhecido fica todo
em `OTHER` e a escolha volta a ser a da primeira menção.

---

## D18 — Formação e idioma procurados no currículo, fora do score

Implementado em `ProfileEvidence`.

Os trechos de formação e idioma da vaga (D15) continuam fora do score, mas o
programa procura no currículo uma frase que pareça responder: curso superior
("Tecnólogo", "Bacharelado", "Graduação", "Universidade", "Cursando"), de
preferência na seção de formação; ou o mesmo idioma citado na vaga, de
preferência na seção de idiomas. A tela mostra "No currículo: ..." ou "Não
encontrei isso no currículo". Não compara área do curso nem nível do idioma.

---

## D19 — O número se chama cobertura técnica

Implementado em `index.html`, `app.js` e `LocalSummaryGenerator`.

O número mede quanto dos requisitos técnicos reconhecidos o currículo
comprova. Comparado com notas dadas por opinião, ele foi lido como "nota do
candidato", o que não é. A tela passou a chamá-lo de "Cobertura técnica da
vaga", o resumo diz "Cobertura tecnica X/100" e o aviso final diz que ele não
é nota do candidato nem probabilidade de contratação. A tela também mostra
quantos obrigatórios têm alguma evidência e quais não têm nenhuma. O campo da
API continua `score`, para não quebrar quem já usa.

---

## D20 — Texto escondido no PDF

Implementado em `VisibilityTextStripper`, `PdfTextExtractor`,
`HiddenTextReport` e `app.js`.

Problema real: palavras-chave em texto invisível ou branco são um truque
conhecido para enganar triagem automática. Um PDF com "Kubernetes Terraform
AWS" escondido ganhava pontos por termos que ninguém lê.

Regra, olhando cada letra durante a extração:

```
modo de desenho invisível (NEITHER)   -> fica fora da análise, aviso
transparência total (alpha < 0,05)    -> fica fora da análise, aviso
cor branca (R, G e B >= 242)          -> entra na análise, aviso
fonte menor que 4 pt                  -> entra na análise, aviso
posição fora da página                -> entra na análise, aviso
```

Branco, minúsculo e fora da página só geram aviso porque podem ser legítimos:
letra branca sobre um cabeçalho escuro é comum em modelos de currículo, e um
PDF gerado com linha mais larga que a página não esconde nada de propósito.
Invisível e transparente não têm uso legítimo em currículo digital, com uma
exceção: o PDF escaneado com camada de texto (OCR) tem todo o texto invisível
por cima da imagem. Por isso, quando 90% ou mais do texto é invisível, nada é
tirado e a tela avisa que o PDF parece escaneado.

O extrator padrão do PDFBox não acompanha a cor; os operadores de cor são
registrados no `VisibilityTextStripper`. O aviso só aparece a partir de 10
caracteres sem espaço em cada caso, para não reagir a ruído do PDF. A API
manda as contagens e o começo de cada trecho em `pdfCheck`; a análise de
texto colado manda `null`.

---

## D21 — Simulação de melhora fora da cobertura

Implementado em `ImprovementSimulator`, `ImprovementHints`, `AnalysisService`
e `app.js`.

A tela ganhou um segundo painel que mostra quais mudanças no currículo
aumentariam a cobertura e quantos pontos cada uma daria. Ele não altera a
cobertura real e não escreve o currículo.

A conta usa o mesmo `ScoreCalculator` da cobertura real, sobre uma cópia da
lista de requisitos com a mudança aplicada. Não existe segunda fórmula.

```
A  PARTIAL                          -> MATCH  "Se você usou X em algum projeto..."
B  MISSING com indício no currículo -> MATCH  "Seu currículo tem X. Se você trabalha com Y..."
C  MISSING sem indício              -> fora da conta, "Sem indício no currículo"
```

C não se chama "lacuna real" porque não é certeza: a extração ainda separa
alternativas que deviam ser um requisito só (L24). Antes da família
{Maven, Gradle}, "Maven ou Gradle" virava dois obrigatórios, e quem tinha
Maven via Gradle em C.

Pontos de cada sugestão = cobertura com só aquela mudança menos a cobertura
real, arredondada em uma casa. "Fazendo todas" aplica todas juntas na mesma
cópia. As sugestões aparecem da que dá mais pontos para a que dá menos.

Como cada número já vem arredondado, a cobertura real mais a soma das
sugestões pode diferir do total em alguns décimos (29,4 + 11,8 + 11,8 + 5,9 +
5,9 + 5,9 = 70,7, e o total é 70,6). A conta não muda: o total vem do
`ScoreCalculator` aplicado a todas as mudanças juntas. A tela e o "Copiar
resultado" avisam que os valores são arredondados.

Indícios são de mão única e ficam numa tabela à parte (`ImprovementHints`),
consultada pelo requisito que falta:

```
Git        <- GitHub
Back-end   <- Java, Spring Boot, API REST
Front-end  <- React, HTML, CSS, JavaScript
```

Java indica Back-end, mas Back-end não indica Java: saber que a pessoa faz
back-end não diz a linguagem. O indício é procurado com o mesmo
`RequirementMatcher` da análise, então termo negado ("Nunca utilizei Java") ou
dentro de e-mail não vale como indício.

Um conjunto PARTIAL com alguma parte ausente ("TCP/IP + DNS + DHCP" sem DHCP)
vai para C, e não para A: descrever melhor as partes que existem não o faz
virar MATCH, porque a parte que falta continua faltando.

Back-end e Front-end entraram no dicionário (com `backend`, `back end`,
`frontend`, `front end`), porque a tabela precisa deles. Com isso, uma vaga
que cita esses termos passa a ter esses requisitos, e a cobertura dessa vaga
muda (ver L23).

A tela continua chamando o número de cobertura (D19): o total aparece como
"Fazendo todas: a cobertura iria de X para Y/100".

---

## Observações do texto real (Bloco 1)

Extraídas de um único currículo real, 3050 caracteres, coluna única. Sustentam
as decisões D1 e D2.

**O1.** Quebra de linha por wrap visual do PDF é real. A frase crítica do
Docker coube inteira numa linha, mas outras quebraram no meio, deixando o termo
órfão do verbo que o qualifica.

**O2.** O padrão lista versus frase é quase binário neste currículo: listas na
forma `Rótulo: item, item.` sem verbo conjugado; frases de projeto sempre com
bullet seguido de verbo em 1ª pessoa. Facilita a regra, mas é risco de
sobreajuste (ver L3 em [LIMITACOES.md](LIMITACOES.md)).

**O3.** Armadilha encontrada. O resumo diz `Possuo conhecimentos em redes de
computadores, ... Microsoft Office, Docker, Azure, Git/GitHub, Sistemas
operacionais Linux e Windows...`. É semanticamente uma lista e deve ser
PARTIAL, mas contém o verbo "Possuo". Se a lista de verbos incluísse verbos de
posse, esta única linha daria MATCH indevido a oito tecnologias de uma vez.

**O4.** Os marcadores de bullet são inconsistentes: `•  Desenvolvi` com dois
espaços e `• Implementei` com um.

**O5.** As linhas de competência terminam em ponto final, então um separador de
frases ingênuo as trata como frases legítimas. Só a exigência de verbo as
mantém corretamente em PARTIAL.
