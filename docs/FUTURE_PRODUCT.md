# Visão do produto futuro

Ideias registradas, **não implementadas** nesta POC. A ordem abaixo é uma
possibilidade, não um compromisso.

## Melhorar a evidência

- **Contagem de ocorrências e detecção de seção.** Saber se o termo aparece em
  "Experiência", "Projetos" ou apenas em "Competências" permite sugerir "cite
  mais X no trecho de projetos". Toda sugestão continua condicional: o sistema
  não sabe o que a pessoa viveu.
- **Requisitos obrigatórios, desejáveis e diferenciais.** A vaga costuma separar
  esses grupos; hoje todos entram com o mesmo peso. Um peso por grupo tornaria o
  score mais próximo da leitura humana.
- **Confidence score por match.** Hoje o status é binário por frase. Uma medida
  de confiança poderia separar evidência forte de evidência fraca.
- **Semantic matching por embeddings.** "Criação de serviços HTTP" e "APIs REST"
  descrevem a mesma experiência com palavras diferentes. Um modelo de embeddings
  capturaria isso sem depender de aliases cadastrados à mão.

## Melhorar o produto

- **Análise de ATS.** Estrutura, seções, legibilidade e formatação do PDF, sem
  prometer que um ATS específico vai ler o documento da mesma forma.
- **Um currículo contra N vagas.** Comparar o mesmo currículo com várias vagas e
  ranquear as que mais combinam.
- **Banco de dados.** A ordem de dor é: primeiro o dicionário de aliases vira
  arquivo JSON/CSV externo, editável sem recompilar; depois, se houver usuários,
  H2 para aprender e MySQL para valer.

## Privacidade e LGPD

Enquanto a aplicação não armazena currículo, o dado pessoal não persiste. No dia
em que passar a salvar currículo, o projeto guarda dado pessoal de terceiro e
entra no escopo da LGPD: base legal, finalidade, retenção, exclusão e segurança
precisam ser tratadas antes de qualquer usuário real.

## Princípio

> Não apenas diga o quanto o currículo combina com a vaga. Mostre por quê.
