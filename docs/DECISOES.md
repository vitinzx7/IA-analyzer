# Decisões travadas

Decisões que já foram tomadas e não devem ser rediscutidas sem evidência nova.
Cada relatório de bloco aponta para cá em vez de repetir esta lista.

Última atualização: requisitos de alternativas (D11).

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
MISSING = o termo não ocorre
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

**Passada 2, separar frases** sobre o texto já remontado, por `.`, `!`, `?` e
`;`, tratando cada item de bullet como frase própria mesmo sem pontuação final.

---

## D3 — Dicionário

Implementado no Bloco 2.

Começar pequeno, cobrindo apenas currículo e vagas de teste, e crescer a partir
dos erros observados no CSV do Bloco 5. Casar sempre o termo mais longo
primeiro, com limite de palavra. Preservar `C#`, `C++`, `.NET` e `Node.js`, que
morrem se a normalização remover pontuação.

Precisa cobrir termos de suporte, infraestrutura, office e RPA, e não apenas
stack de desenvolvimento — as duas vagas reais são de suporte.

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
```

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
score = pontos obtidos / total de requisitos reconhecidos × 100
```

Arredondado para uma casa decimal. Lista vazia ou nula resulta em `0`. O
denominador é sempre o número de requisitos **reconhecidos**, não o número de
requisitos escritos na vaga (ver L2).

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
- A interface está em `/` e usa apenas HTML, CSS e JavaScript, sem framework.

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
```

`Windows/Linux` não agrupa: em vaga de suporte a barra costuma pedir os dois.

O grupo é satisfeito por qualquer opção que tenha evidência, segue a mesma
regra D1 e vale **1** requisito no score. Uma tecnologia que já está num grupo
não entra de novo como requisito sozinha, em qualquer ordem do texto.

**Diferença em relação à primeira proposta.** A proposta original mandava
separar todos os termos quando a lista inteira não fosse da mesma família.
Isso deixaria "Java, Docker, Azure, AWS ou GCP" com os mesmos dois MISSING
falsos. Por isso o agrupamento olha só o trecho final da lista.

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
