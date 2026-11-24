# Limitações conhecidas

Problemas identificados e **não** resolvidos, com o motivo de não terem sido
resolvidos. Cada relatório de bloco aponta para cá em vez de repetir esta lista.

Última atualização: negação do termo, ponto em nomes e score nulo.

Estado: `ABERTA` (não tratada) · `MITIGADA` (tratada em parte) ·
`RESOLVIDA` (com o bloco que resolveu).

---

## L1 — Um verbo concede MATCH a todos os termos da frase, sem teto · ABERTA

A regra D1 não impõe limite de termos por frase. Uma única frase com um verbo
válido promove a MATCH todas as tecnologias que ela contiver.

Ocorrência real no currículo:

> "Desenvolvi e publiquei uma aplicação web com backend Java, banco de dados
> PostgreSQL e containers Docker na Azure Container Apps."

Isso concederá MATCH a Java, PostgreSQL, Docker e Azure a partir de um único
"Desenvolvi". Neste caso o resultado é legítimo — as quatro foram de fato
usadas no projeto.

Risco: uma lista disfarçada de frase, como "Desenvolvi projetos com Java,
Python, Go, Rust, Kubernetes e AWS", produziria seis MATCH indevidos.

**Por que não foi tratada:** medir a frequência real do padrão no CSV do Bloco 5
antes de introduzir mitigação, para não adicionar parâmetro de calibragem sem
evidência.

---

## L2 — Requisito fora do dicionário não é contado · MITIGADA no Bloco 7

O denominador da fórmula de score só inclui requisitos reconhecidos, então o
score pode inflar artificialmente numa vaga que peça tecnologias desconhecidas
pelo sistema.

**Mitigação implementada:** a interface mostra quantos requisitos foram
reconhecidos e o README documenta a limitação. O denominador continua sendo
apenas o que o dicionário conhece; requisitos desconhecidos seguem sem entrar
no cálculo.

---

## L3 — Regra possivelmente sobreajustada a um único currículo · ABERTA

Todos os achados O1 a O5 em [DECISOES.md](DECISOES.md) vêm de um só documento.
O padrão binário de lista versus frase pode não se repetir em currículos de
outra pessoa ou de outro template.

---

## L4 — Cobertura incompleta contra texto real · ABERTA

Nenhuma das duas vagas reais pede Java, Spring, Docker, PostgreSQL ou cloud.
Todo o lado de desenvolvimento do currículo fica sem exercício contra texto
real e ruidoso. Os aliases `Postgres`/`JS`/`k8s` e o par Docker/React só são
exercitados pelas vagas sintéticas, que são limpas e previsíveis.

Tentativa de resolver automaticamente, falhou:

```
$ curl "https://remotive.com/api/remote-jobs?category=software-dev&limit=200"
total devolvido: 18; com java ou spring: 1 ("Senior QA Engineer")
```

LinkedIn, Gupy e Indeed bloqueiam acesso automatizado.

**Depende de:** o usuário colar manualmente uma vaga real de backend Java
júnior ou estágio.

---

## L5 — Denominador pequeno torna o score frágil · ABERTA

A vaga real da Veirano produz apenas **3** requisitos reconhecidos. Com 3
requisitos, cada decisão vale 33,3 pontos e um único erro de classificação
desloca o score de 100 para 67. O score dessa vaga terá pouca resolução e alta
variância.

---

## L6 — Ordenação por comprimento não é ordenação por especificidade · ABERTA

O algoritmo casa o termo mais longo primeiro, mas comprimento de string não
equivale a especificidade semântica.

Caso real, já corrigido pontualmente no Bloco 2: o alias `pacote office` (13
caracteres) vencia o canônico `office 365` (10), fazendo com que
"pacote Office 365" fosse classificado como Microsoft Office. Corrigido com o
alias mais específico `pacote office 365`, mas **a causa raiz permanece** e não
houve varredura sistemática de outros pares.

Varredura parcial executada, apenas sobre nomes canônicos — 9 pares onde um
nome está contido em outro:

```
azure    dentro de  azure container apps
docker   dentro de  docker compose
git      dentro de  github
git      dentro de  gitlab
java     dentro de  javascript
sql      dentro de  mysql
sql      dentro de  postgresql
sql      dentro de  sql server
windows  dentro de  windows server
```

**Importante:** esses 9 pares **não são defeitos**. São exatamente o caso para
o qual o mecanismo de "termo mais longo primeiro" existe, e todos foram
verificados como corretos. O defeito aparece só quando a leitura mais longa é a
**errada**, como no caso do Office 365.

A varredura está incompleta: ela cobre apenas nomes canônicos, porque a API
pública do dicionário não expõe os aliases. Uma varredura completa incluindo
aliases produziria mais pares.

---

## L7 — Alinhamento de índices · RESOLVIDA no Bloco 2

Era: o recorte da evidência dependia de a normalização preservar o comprimento
do texto, o que é falso para acento escrito de forma decomposta (NFD).

O defeito era grave: em texto NFD, `findIn("Manutenção de Docker")` devolvia
`matchedText=[e Dock]` em vez de `[Docker]`, violando a regra R2 na prática.

Resolvido com um mapa de índices que associa cada caractere do texto
normalizado ao trecho do original que o gerou.

---

## L8 — "Spring Boot" não satisfaz uma vaga que peça "Spring" · RESOLVIDA no Bloco 3

`spring` é alias de Spring Framework e `Spring Boot` é canônico separado. Como
o termo mais longo vence e consome a região, um currículo que diga apenas
"Spring Boot" **não** casava com uma vaga que peça "Spring".

Resolvido com equivalência unidirecional em `TechnologyEquivalence`: Spring Boot
satisfaz Spring Framework, mas Spring Framework não satisfaz Spring Boot. A
assimetria preserva a distinção de quem pediu a ferramenta específica. Ver D6.

Verificado:

```
[Spring Boot]      + requisito Spring Framework -> MATCH
[Spring Framework] + requisito Spring Boot      -> MISSING
```

---

## L9 — Composições dependem de alias cadastrado à mão · ABERTA

Depois que a checagem de borda passou a valer para os dois lados, `.net` exige
borda limpa à esquerda. Formas como `ASP.NET` só continuam sendo reconhecidas
porque existe o alias `asp.net` cadastrado manualmente.

Outras composições análogas não previstas deixarão de casar **silenciosamente**.
Não há mecanismo genérico.

---

## L10 — Consumo guloso sem backtracking · ABERTA

Uma forma mais longa que case e consuma a região impede formas menores ali,
mesmo que a leitura menor fosse a correta. Foi exatamente o mecanismo do
defeito do "pacote Office 365". O algoritmo não reavalia decisões tomadas.

**Mitigação parcial:** pares específicos que sofriam com isso ganharam
equivalência unidirecional: `Azure Container Apps` satisfaz `Azure` e
`Docker Compose` satisfaz `Docker`. A causa raiz continua: o algoritmo ainda
não reavalia a escolha, então outros pares podem precisar do mesmo tratamento
manual.

Relacionada a L6, mas não idêntica: L6 é sobre a **ordenação** escolher o
candidato errado; L10 é sobre a **irreversibilidade** da escolha.

---

## L11 — Versão colada ao nome não é reconhecida · RESOLVIDA no Bloco 3

A checagem de borda simétrica rejeitava o termo quando ele encostava em dígito.
Verificado antes da correção:

```
[Projetos em C++11]      -> []          [Projetos em C++ 11]      -> [C++]
[Java21 no curriculo]    -> []          [Java 21 no curriculo]    -> [Java]
[Python3 no script]      -> []
[Windows11 na estacao]   -> []          (a vaga real escreve "Windows 11")
```

**Regressão parcial introduzida no Bloco 2.** O caso `C++11` funcionava antes
da correção da borda simétrica, porque o lado direito não era checado quando o
termo terminava em pontuação. Os casos `Java21`, `Python3` e `Windows11` já
eram rejeitados antes e **não** são regressão.

Resolvido aceitando uma sequência de dígitos à direita somente quando ela não é
seguida por letra: `Java21` e `C++11` casam, `Java2EE` continua rejeitado. A
borda à esquerda e o caso `JavaScript` não mudaram.

---

## L12 — Alternativas dependem de família cadastrada à mão · ABERTA

Só três famílias existem (nuvem, banco relacional, hospedagem de Git). Uma vaga
com "Java ou Kotlin" ou "React ou Angular" continua gerando dois requisitos, e
quem tem só um deles recebe um MISSING falso.

A leitura da lista também é por padrão de texto: "ou", "e/ou" e "/" abrem
alternativa; "e" não. "Azure e/ou AWS" agrupa; "Azure e AWS" não.

**Por que não foi tratada:** crescer as famílias pelos erros vistos no CSV, como
o dicionário (D3), em vez de prever todos os pares.

---

## L13 — Máscara cobre só e-mail e telefone · ABERTA

A máscara de evidência (D12) troca e-mail e telefone. Nome, CPF, endereço e
links de perfil continuam aparecendo se estiverem na frase usada como
evidência. Verificado:

```
"CPF 123.456.789-00"     -> sem máscara
"Matricula 2023101234"   -> "Matricula [telefone]"   (falso positivo: 10 dígitos
                                                       têm o formato de DDD + número)
```

Uma linha com link nunca é juntada com a seguinte (D2). Num bullet de projeto
que tenha link e quebre de linha, a continuação vira frase separada e pode
perder o verbo:

```
"• Publiquei o codigo em github.com/x/y com backend em\nJava."
   -> "Java." fica sozinha
```

**Por que não foi tratada:** o pedido cobria e-mail e telefone. CPF e nome
exigem regra própria, e a exceção para bullets com link ainda não tem caso real
no CSV.

---

## L14 — Negação só é reconhecida em duas formas · ABERTA

A negação do termo (D1) só pega a negação logo antes do termo ("sem Docker") e
a oração em que todos os verbos da lista estão negados ("Nunca utilizei AWS").
Casos que **não** são pegos:

```
"Desenvolvi o front, sem experiência em Java."  -> Java MATCH (negação longe do termo)
"Nunca utilizei AWS, GCP ou Azure."             -> GCP e Azure PARTIAL (outra oração
                                                   por causa da vírgula)
```

**Por que não foi tratada:** pegar esses casos exige janela de N palavras ou
análise sintática, e a D1 recusa número arbitrário sem evidência.

---

## L15 — Ponto colado antes de minúscula nunca separa frase · ABERTA

Para não cortar `Node.js` e `github.com/x/y`, um ponto colado entre letra e
minúscula nunca separa frase. Um erro de digitação como "Desenvolvi a
API.depois configurei" vira uma frase só.

---

## L16 — Tempo quadrático em texto com muitas repetições · RESOLVIDA

O matcher avaliava o verbo da frase inteira uma vez para cada ocorrência do
termo. Um texto com o mesmo termo repetido milhares de vezes travava a análise.
Medido no código anterior (`feature/mvp`, `7b339d3`) e depois da correção, com
"Java, " repetido N vezes:

```
N        antes      depois
2.000    6.656 ms   271 ms (inclui aquecimento da JVM)
4.000   24.060 ms   121 ms
8.000   97.178 ms   121 ms
33.000  > 180 s     426 ms
```

Resolvido avaliando cada frase e cada oração uma vez só por análise e achando
a frase de cada ocorrência por busca binária. Um teste com tempo limite de 5 s
protege contra regressão.
