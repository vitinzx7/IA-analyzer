# Resume Match

POC que compara um currículo em PDF com a descrição de uma vaga e mostra **por
que** cada requisito foi considerado atendido, parcial ou ausente.

O sistema extrai o texto do PDF, reconhece tecnologias da vaga com um dicionário
local e classifica cada requisito pela evidência encontrada no currículo. Não há
modelo de linguagem, chamada externa nem banco de dados: a análise é determinística
e roda inteira na máquina.

> This repository currently contains a Proof of Concept.
> The scoring algorithm should not be interpreted as a real hiring
> probability or as a simulation of a specific ATS.

## Requisitos

- Java 21
- Maven (o wrapper `./mvnw` já está no repositório)

## Como rodar

```bash
./mvnw spring-boot:run
```

Abra <http://localhost:8080>. Envie o currículo em PDF (ou cole o texto dele na
aba **Colar texto**), cole a descrição da vaga e clique em **Analisar** ou use
`Ctrl` + `Enter`. O resultado substitui o formulário: à esquerda fica o resumo
(cobertura, contagem por status e avisos) e à direita três abas. **Requisitos**
mostra o trecho de evidência de cada requisito, com filtro por status.
**Como melhorar** mostra a simulação de melhora. **Não avaliado** mostra o que a
vaga pede e o programa não avaliou. **Editar análise** volta ao formulário com
os dados preenchidos, e **Copiar resultado** copia tudo como texto.

Para gerar o JAR:

```bash
./mvnw clean package
java -jar target/resume-match-0.0.1-SNAPSHOT.jar
```

## API

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/health` | Verifica se a aplicação está no ar. |
| `POST` | `/api/analyze` | Analisa um PDF (`multipart/form-data`). |
| `POST` | `/api/analyze/text` | Analisa texto puro (`application/json`). |

Exemplo com PDF:

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "resume=@curriculo.pdf;type=application/pdf" \
  --form-string "job=Requisitos: Java, Spring Boot, Docker e AWS."
```

Exemplo com texto:

```bash
curl -X POST http://localhost:8080/api/analyze/text \
  -H "Content-Type: application/json" \
  -d '{
    "resume": "Desenvolvi uma API em Java. Tecnologias: Docker, Git.",
    "job": "Java, Docker, AWS"
  }'
```

Resposta:

```json
{
  "score": 50.0,
  "requirements": [
    { "name": "Java", "status": "MATCH", "evidence": "Desenvolvi uma API em Java.",
      "priority": "REQUIRED", "components": [] },
    { "name": "Docker", "status": "PARTIAL", "evidence": "Tecnologias: Docker, Git.",
      "priority": "REQUIRED", "components": [] },
    { "name": "AWS", "status": "MISSING", "evidence": null,
      "priority": "REQUIRED", "components": [] }
  ],
  "summary": "Score 50/100: ...",
  "unevaluated": [],
  "unknownTerms": []
}
```

- `priority`: `REQUIRED` (obrigatório, peso 1) ou `PREFERRED` (diferencial,
  peso 0,5).
- `components`: decisão de cada parte quando o requisito é um conjunto, como
  `TCP/IP + DNS + DHCP`.
- `unevaluated`: trechos de requisito da vaga que não entraram no score, com
  a seção e o motivo (`EDUCATION`, `LANGUAGE` ou `NO_KNOWN_TERM`).
- `unknownTerms`: nomes da vaga que parecem tecnologia mas o dicionário não
  conhece.
- `improvement`: simulação de melhora (ver abaixo). Não muda o `score`.

Erros seguem o formato `ProblemDetail` (RFC 9457), com `title` e `detail`.

## Como o score funciona

Cada requisito reconhecido recebe um status:

```
MATCH   = o termo aparece em uma frase com verbo de ação em 1ª pessoa do passado
PARTIAL = o termo aparece, mas só em listas ou frases sem verbo de ação
MISSING = o termo não aparece, ou só aparece negado ("sem Docker",
          "Nunca utilizei AWS")
```

Os verbos considerados são: desenvolvi, implementei, criei, publiquei, realizei,
utilizei, configurei, apoiei, mantive, escrevi.

Uma vaga que diga "Azure, AWS ou GCP" gera um único requisito de alternativas,
satisfeito por qualquer uma das três (ver D11 em `docs/DECISOES.md`). Termos
entre parênteses, como "noções de redes (TCP/IP, DNS, DHCP)", viram um único
conjunto, avaliado parte por parte (D14).

A vaga é lida por seção (D13). O que está em "Diferenciais", ou num trecho
com "desejável", vale metade. O que está em benefícios, local e horário não é
requisito. Formação, idioma e trechos sem termo do dicionário aparecem na
tela como "não avaliado" (D15).

O score é a média ponderada dos status, em uma escala de 0 a 100:

```
MATCH = 1,0 ponto · PARTIAL = 0,5 ponto · MISSING = 0 ponto
peso: obrigatório = 1 · diferencial = 0,5
score = Σ (peso × pontos) / Σ peso × 100
```

O resultado é arredondado para uma casa decimal. Vaga sem nenhum requisito
reconhecido não tem nota: a API devolve `"score": null` e o resumo explica.

Na tela, o número se chama **cobertura técnica da vaga** (D19): ele diz quanto
dos requisitos técnicos reconhecidos o currículo comprova, e não é nota do
candidato. A tela também lista os obrigatórios sem nenhuma evidência.

Quando um termo aparece em mais de um lugar do currículo, a evidência mostrada
vem da seção mais forte (D17): experiência e projetos, depois cursos e
formação, depois competências, resumo e, por último, o cabeçalho. A API manda
a seção em `evidenceSection`. Formação e idioma pedidos pela vaga não entram
no cálculo, mas a tela mostra o trecho do currículo que parece responder
(`resumeEvidence`, D18).

O resumo é gerado localmente com frases condicionais. O sistema não sabe o que a
pessoa realmente viveu, então nunca manda inventar experiência: quando sugere
algo, escreve "se você realmente tem experiência com X, considere deixar isso
explícito no currículo".

## Registro em CSV

Cada análise concluída grava uma linha por requisito em `analises.csv`:

```csv
data,requisito,status,evidencia
2026-11-23,Java,MATCH,Desenvolvi uma API em Java.
```

O arquivo é local, está no `.gitignore` e pode conter trechos do currículo.
E-mail e telefone saem mascarados como `[email]` e `[telefone]`, e um campo que
comece com `=`, `+`, `-` ou `@` recebe um apóstrofo para não virar fórmula na
planilha.
Para desligar o registro ou mudar o caminho:

```properties
resumematch.csv.enabled=false
resumematch.csv.path=/caminho/analises.csv
```

## Memória de vagas

Cada análise também grava **uma linha por vaga** em `vagas.csv`, para comparar
vagas e procurar padrões, como o que mais pedem ou o que mais falta no
currículo:

```csv
data,chave,cargo,empresa,local,link,cobertura,obrigatorios_atendidos,obrigatorios_parciais,obrigatorios_faltando,diferenciais_atendidos,diferenciais_parciais,diferenciais_faltando,termos_desconhecidos
2026-11-28 15:12:24,linkedin:4484297425,Desenvolvedor Java,Empresa Exemplo,"Brasília, DF · Híbrido",https://www.linkedin.com/jobs/view/4484297425/,50.0,Java; Spring Boot,SQL; Git,Back-end,,,Docker; AWS,
```

- **chave:** o número da vaga quando o texto tem link do LinkedIn; sem link, um
  resumo (SHA-256) do texto. A mesma vaga analisada de novo ganha outra linha
  com a mesma chave.
- **cargo, empresa, local:** lidos só quando o texto começa com essas linhas
  seguidas do link sozinho numa linha. Fora desse formato ficam vazios.
- **termos_desconhecidos:** nomes que parecem tecnologia mas o dicionário não
  conhece; servem para saber o que falta cadastrar.

Na tela de entrada, o painel **O que as vagas analisadas pedem** conta, para
cada requisito, em quantas vagas diferentes ele aparece e se é obrigatório ou
diferencial (`GET /api/patterns`). Cada vaga conta uma vez, pela análise mais
recente. Ao analisar uma vaga que já está na memória, o resumo avisa quantas
vezes ela foi analisada, quando foi a última e quanto a cobertura mudou.

O arquivo é local, está no `.gitignore` e não guarda trecho do currículo.
Para desligar ou mudar o caminho:

```properties
resumematch.jobs.enabled=false
resumematch.jobs.path=/caminho/vagas.csv
```

## Texto escondido no PDF

Ao ler o PDF, o programa olha como cada letra é desenhada (D20). Texto em modo
invisível ou totalmente transparente fica **fora** da análise, porque é o jeito
de esconder palavras-chave. Texto branco, em fonte menor que 4 pt ou fora da
página entra, mas gera aviso na tela: pode ser legítimo, como letra branca
sobre um cabeçalho escuro. Um PDF escaneado com camada de texto (OCR) não
perde nada. A API manda o resultado em `pdfCheck`:

```json
"pdfCheck": {
  "invisibleChars": 33, "whiteChars": 0, "tinyChars": 0, "outsideChars": 0,
  "scannedLayer": false,
  "invisibleSample": "Kubernetes Terraform AWS Splunk Wazuh",
  "suspiciousSample": ""
}
```

## Simulação de melhora

Um segundo painel mostra quais mudanças no currículo aumentariam a cobertura
e quantos pontos cada uma daria (D21). A cobertura real não muda e o programa
não escreve o currículo: como ele não sabe se a pessoa usou o termo, o texto é
sempre condicional.

- **A, só em lista:** o requisito aparece sem frase de ação. "Se você usou
  React em algum projeto, descreva isso numa frase de ação."
- **B, indício:** o requisito falta, mas outro termo do currículo o sugere.
  "Seu currículo tem Java. Se você trabalha com Back-end, cite Back-end numa
  frase de ação." Os indícios são de mão única: Java indica Back-end, mas
  Back-end não indica Java.
- **Sem indício no currículo:** falta, e nenhum termo do currículo sugere.
  Não dá pontos. Não é certeza de lacuna: a vaga pode aceitar uma alternativa
  que o programa separou em outro requisito.

Os pontos saem da mesma conta da cobertura, aplicada numa cópia dos
requisitos com a mudança. Como cada número vem arredondado em uma casa, a
soma das sugestões pode diferir do total "fazendo todas" em alguns décimos.
Na API:

```json
"improvement": {
  "suggestions": [
    { "requirement": "Back-end", "type": "INDICATED", "indicator": "Java",
      "evidence": "Desenvolvi uma API em Java.", "priority": "REQUIRED",
      "scoreWithChange": 62.5, "points": 25.0 },
    { "requirement": "Docker", "type": "LIST_ONLY", "indicator": null,
      "evidence": "Tecnologias: Docker.", "priority": "REQUIRED",
      "scoreWithChange": 50.0, "points": 12.5 }
  ],
  "scoreWithAll": 75.0,
  "gaps": [ { "requirement": "AWS", "priority": "REQUIRED", "missingParts": [] } ]
}
```

## Medir o acerto

Cada requisito do resultado tem os botões "Certo" e "Devia ser …". Cada
marcação grava uma linha em `feedback.csv`, também local e fora do Git:

```csv
data,analise,requisito,status_programa,status_correto
2026-11-24,cb7317ba-...,Docker,PARTIAL,MISSING
```

O arquivo não guarda trecho do currículo, só o nome do requisito e os dois
status. A tela mostra o acerto acumulado, e a API também:

```bash
curl http://localhost:8080/api/feedback/summary
```

```json
{
  "total": 2, "correct": 1, "accuracy": 50.0,
  "byStatus": {
    "MATCH":   { "marked": 1, "markedCorrect": 1, "precision": 100.0, "expected": 1, "found": 1, "recall": 100.0 },
    "PARTIAL": { "marked": 1, "markedCorrect": 0, "precision": 0.0,   "expected": 0, "found": 0, "recall": null },
    "MISSING": { "marked": 0, "markedCorrect": 0, "precision": null,  "expected": 1, "found": 0, "recall": 0.0 }
  }
}
```

- **Precisão** de um status: das vezes em que o programa disse aquilo, quantas
  estavam certas. MATCH sem merecer derruba a precisão de MATCH (falso
  positivo).
- **Revocação**: das vezes em que aquele era o status certo, quantas o programa
  acertou. MISSING com evidência no currículo derruba a revocação de MATCH e
  PARTIAL (falso negativo).

Marcar o mesmo requisito de novo na mesma análise substitui a marcação
anterior. Para desligar ou mudar o caminho:

```properties
resumematch.feedback.enabled=false
resumematch.feedback.path=/caminho/feedback.csv
```

## Limitações

- **Requisito fora do dicionário não é contado.** O denominador do score inclui
  apenas os termos que o dicionário conhece. Numa vaga com tecnologias
  desconhecidas o score pode parecer melhor do que é. A interface mostra quantos
  requisitos foram reconhecidos.
- O dicionário é pequeno e cresce a partir dos erros observados. Ele cobre
  principalmente desenvolvimento backend, suporte, infraestrutura, office e RPA.
- A classificação depende de frases com verbo em 1ª pessoa do passado. Currículos
  escritos em terceira pessoa ou em tópicos sem verbo tendem a gerar `PARTIAL`.
- Um verbo em uma frase concede `MATCH` a todos os termos daquela frase.
- O score **não** é probabilidade de contratação, não simula ATS e não avalia
  formação, senioridade, localização ou pretensão salarial.
- O PDF é processado em memória e não é armazenado. O CSV guarda apenas os
  trechos usados como evidência.

## Privacidade

- O currículo enviado não é salvo pela aplicação.
- O CSV de análise e a memória de vagas ficam na máquina e estão ignorados
  pelo Git. A memória de vagas não guarda trecho do currículo.
- Nenhum texto é enviado para serviços externos.

## Testes

```bash
./mvnw test
```

A suíte cobre extração de PDF, normalização e dicionário, divisão em frases,
extração de requisitos, classificação, score, simulação de melhora, resumo, CSV,
API e a página web.

## Estrutura

```
src/main/java/com/resumematch/
├── analysis/   orquestração da análise
├── api/        controller, configuração e tratamento de erros
├── dictionary/ dicionário de tecnologias e normalização
├── log/        registro CSV
├── matcher/    frases, requisitos, classificação, score, simulação e resumo
└── pdf/        extração de texto do PDF
```

Decisões e limitações detalhadas ficam em [`docs/DECISOES.md`](docs/DECISOES.md)
e [`docs/LIMITACOES.md`](docs/LIMITACOES.md). A visão de produto futuro está em
[`docs/FUTURE_PRODUCT.md`](docs/FUTURE_PRODUCT.md).
