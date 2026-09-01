# Decisões travadas

Decisões que já foram tomadas e não devem ser rediscutidas sem evidência nova.
Cada relatório de bloco aponta para cá em vez de repetir esta lista.

Última atualização: Bloco 2.

---

## D1 — Regra de classificação MATCH / PARTIAL / MISSING

A implementar no Bloco 3, pelo desenvolvedor humano.

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

A implementar no Bloco 3.

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
