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
`Ctrl` + `Enter`. O resultado mostra o score, a contagem por status, o trecho de
evidência de cada requisito com o termo destacado e um botão para copiar o
resultado como texto.

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
    { "name": "Java", "status": "MATCH", "evidence": "Desenvolvi uma API em Java." },
    { "name": "Docker", "status": "PARTIAL", "evidence": "Tecnologias: Docker, Git." },
    { "name": "AWS", "status": "MISSING", "evidence": null }
  ],
  "summary": "Score 50/100: ..."
}
```

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
satisfeito por qualquer uma das três (ver D11 em `docs/DECISOES.md`).

O score é a média ponderada dos status, em uma escala de 0 a 100:

```
MATCH = 1,0 ponto · PARTIAL = 0,5 ponto · MISSING = 0 ponto
score = pontos obtidos / pontos possíveis × 100
```

O resultado é arredondado para uma casa decimal. Vaga sem nenhum requisito
reconhecido não tem nota: a API devolve `"score": null` e o resumo explica.

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
- O CSV de análise fica na máquina e está ignorado pelo Git.
- Nenhum texto é enviado para serviços externos.

## Testes

```bash
./mvnw test
```

A suíte cobre extração de PDF, normalização e dicionário, divisão em frases,
extração de requisitos, classificação, score, resumo, CSV, API e a página web.

## Estrutura

```
src/main/java/com/resumematch/
├── analysis/   orquestração da análise
├── api/        controller, configuração e tratamento de erros
├── dictionary/ dicionário de tecnologias e normalização
├── log/        registro CSV
├── matcher/    frases, requisitos, classificação, score e resumo
└── pdf/        extração de texto do PDF
```

Decisões e limitações detalhadas ficam em [`docs/DECISOES.md`](docs/DECISOES.md)
e [`docs/LIMITACOES.md`](docs/LIMITACOES.md). A visão de produto futuro está em
[`docs/FUTURE_PRODUCT.md`](docs/FUTURE_PRODUCT.md).
