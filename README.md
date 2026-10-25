# java-spring-create-module — como usar com uma IA

Skill que **cria (ou revisa) um módulo de negócio** em um projeto Java Spring Boot **que já existe**, a partir do contrato OpenAPI. Você escolhe o recurso do contrato; a IA implementa controller, caso de uso, portas, adaptadores, logs e testes, e roda a verificação. Você não roda scripts à mão: pede a uma ferramenta de IA (Claude Code, Codex, Copilot etc.) e informa os dados abaixo. As instruções que a IA segue estão em [SKILL.md](SKILL.md).

## 1. Para que serve

| Faz | Não faz |
|---|---|
| Implementa operações já definidas no contrato OpenAPI | Criar o projeto (use `java-spring-scaffold`) |
| Cria o módulo na estrutura do projeto (`api`, `application`, `domain`, `infrastructure`) | Escrever ou alterar o contrato OpenAPI (use `openapi-rest-contract`) |
| Um caso de uso concreto por operação (`<Verbo><Substantivo>UseCase`) | Inventar endpoints ou regras de negócio que o contrato não define |
| Logs de evento de negócio limpos e rastreáveis | Alterar o contrato sem pedido explícito |
| Testes do módulo e verificação com `mvnw clean verify` | Editar `target/generated-sources` |
| **Modo revisão**: audita um módulo existente e relata achados | Mudar código no modo revisão, a menos que você peça |

## 2. Pré-requisitos

- **Projeto Java Spring Boot já criado**, de preferência pela skill `java-spring-scaffold` (Maven Wrapper, Spring Modulith, Clean Architecture). Em outros projetos a skill segue a estrutura que encontrar.
- **Contrato OpenAPI existente** no projeto, normalmente em `src/main/resources/openapi/openapi.yaml` (módulos em `openapi/modules/`). Se não houver, a IA vai pedir o caminho; ela não cria o contrato.
- JDK compatível com o projeto (21 ou 25) e `JAVA_HOME` configurado. Use o `mvnw` do projeto, não um Maven global.
- Internet no primeiro build (o wrapper baixa Maven e dependências).
- Node.js 20.19+ ou 22.12+ **só** se for usar o inspetor de contrato opcional (veja a seção 6).
- O repositório sem alterações pendentes que você não queira misturar com o módulo (a IA preserva o que não for dela, mas um estado limpo facilita a revisão).

## 3. Como apontar o projeto

A skill trabalha **no projeto onde a IA está aberta**. Há duas formas:

1. **Abra a ferramenta de IA na pasta do projeto** (a que contém o `pom.xml`, por exemplo `api-finance-controller/`). É o jeito mais simples.
2. **Informe o caminho no pedido**, se a IA estiver aberta em outra pasta (por exemplo na raiz de um monorepo):
   `Projeto: C:\projetos\meu-monorepo\api-finance-controller`.

Se a IA for aberta na raiz de um monorepo e você não disser o caminho, ela deve perguntar qual pasta contém o `pom.xml`.

## 4. Informações que você deve passar

| Informação | Obrigatória | Observação |
|---|---|---|
| Recurso a implementar | **Sim** | Módulo/tag, `operationId` ou método + caminho. Ex.: `tag categorias`, `createCategory`, `GET /categorias`. Se faltar, a IA **pergunta**; ela nunca escolhe por você |
| Caminho do projeto | Só se a IA não estiver aberta nele | Pasta com o `pom.xml` |
| Caminho do contrato | Só se não estiver no local padrão | Ex.: `src/main/resources/openapi/openapi.yaml` |
| Regras de negócio e persistência | Quando o contrato não as deixa claras | Ex.: o que fazer ao excluir uma categoria em uso. A IA pergunta só o que bloqueia |
| Modo | Opcional | `criar` (padrão) ou `revisar` |

Um nome de recurso basta quando ele seleciona um conjunto claro de operações. Se o nome combinar com mais de um módulo, a IA mostra os candidatos e pede que você escolha.

## 5. Como acionar

### Modelo de pedido (serve para qualquer ferramenta)

```text
Use a skill java-spring-create-module para implementar um módulo.

- Projeto: C:\projetos\meu-monorepo\api-finance-controller
- Recurso: tag "categorias" (todas as operações)
- Regras: categoria em uso não pode ser excluída (409)

Rode a verificação ao final e me mostre o resultado.
```

Para revisar um módulo existente:

```text
Use a skill java-spring-create-module em modo revisão no módulo "categorias" do projeto
C:\projetos\meu-monorepo\api-finance-controller. Só relate os achados, não altere código.
```

Se faltar algum dado, a IA deve perguntar.

### Claude Code

Coloque a pasta da skill em `~/.claude/skills/java-spring-create-module` (ou em `.claude/skills/` do repositório). Abra o Claude Code na pasta do projeto e peça:

```text
/java-spring-create-module
Recurso: tag categorias
```

Ou escreva o modelo acima em linguagem natural; a skill é escolhida pela descrição.

### Codex

A skill já está em `~/.codex/skills/java-spring-create-module`. Abra o Codex na pasta do projeto e cite-a pelo nome (por exemplo `$java-spring-create-module`) ou use o modelo de pedido.

### GitHub Copilot (Agent mode) e outras ferramentas

Se a ferramenta não carregar skills automaticamente, dê o caminho da skill e peça que siga as instruções:

```text
Leia C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md e siga as instruções
para implementar o recurso "categorias" neste projeto.
```

A ferramenta precisa ler e editar arquivos e executar comandos no terminal (`mvnw`, e `node` se usar o inspetor).

## 6. O que a IA faz, em ordem

1. Localiza o projeto, o contrato, o Maven Wrapper, os módulos existentes e a configuração do OpenAPI Generator.
2. Resolve o escopo (por caminho/método, `operationId` ou tag) e lê os schemas, status e interfaces geradas. Se os arquivos gerados não existirem, roda `generate-sources`.
3. Implementa **só** as operações selecionadas, na estrutura do projeto.
4. Adiciona uma linha de log de evento de negócio por caso de uso que altera estado e escreve os testes.
5. Roda `mvnw clean verify`, o teste de fronteiras do Modulith e, se o ambiente permitir, sobe a aplicação e chama os endpoints.
6. Reporta arquivos alterados, operações cobertas, verificações aprovadas e decisões pendentes.

**Inspetor de contrato (opcional).** Para listar as operações antes de escolher, a IA pode rodar `node <skill>/scripts/inspect-contract.mjs --entry <openapi.yaml> [--resource <seletor>]`. Na primeira vez, é preciso `npm ci` dentro da pasta da skill.

## 7. Estrutura que o módulo recebe

```text
<modulo>/
  api/                      controller (implementa a interface gerada) e mapper
  application/
    usecase/                <Verbo><Substantivo>UseCase, um por operação (@Service, execute, @Transactional)
    port/out/               interfaces pequenas do que a aplicação precisa de fora
  domain/                   modelos e regras, sem Spring nem DTOs gerados
  infrastructure/           adaptadores das portas de saída
  package-info.java         @ApplicationModule
```

Regras principais: sem sufixo `Service`, sem interface por caso de uso, DTOs gerados ficam na borda, todo acesso a dados filtra pelo proprietário (`owner_id`), que vem do contexto autenticado e nunca do cliente. Detalhes em [SKILL.md](SKILL.md) e [references/](references/).

## 8. Logs

Cada caso de uso que altera estado emite **uma** linha de evento de negócio com pares chave-valor (SLF4J), sem dados pessoais, sem log em controller, domínio ou mapper e sem registrar a mesma exceção duas vezes. O `requestId` e o `traceId` vêm do projeto (filtro do scaffold). Regras completas em [references/logging.md](references/logging.md).

## 9. O que esperar como resultado

O módulo implementado e testado, o relatório de verificação (build, testes, fronteiras do Modulith, inicialização e chamadas aos endpoints, quando possível) e a lista de pendências, se houver. Se algo falhar, a IA deve informar o comando, o erro e o caminho do log, em vez de declarar sucesso. Um módulo com comportamento ainda em placeholder **não** é dado como concluído.

## 10. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| A IA pergunta qual recurso implementar | Esperado: você não indicou o recurso. Passe módulo/tag, `operationId` ou `GET /caminho` |
| "Contrato não encontrado" | Informe o caminho do `openapi.yaml`; a skill não cria contrato |
| Interfaces ou DTOs gerados ausentes | Rode `.\mvnw.cmd generate-sources` (a IA faz isso quando há gerador configurado) |
| `mvn` não reconhecido | Use `.\mvnw.cmd` (Windows) ou `./mvnw`; o projeto usa Maven Wrapper |
| Projeto não reconhecido na IDE | Abra a pasta que contém o `pom.xml` ou adicione-o como projeto Maven |
