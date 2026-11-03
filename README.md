# java-spring-create-module - como usar com uma IA

Skill que **cria, migra ou revisa um módulo de negócio** (uma *feature*) em um projeto Java Spring Boot **que já existe**, no modelo **"Hexagonal Pragmática"**: pacote por feature, domínio puro no centro, uma classe de caso de uso por operação, portas só para saídas, `api` como adaptador de entrada e `infrastructure` (JPA, clients, mensageria) como adaptadores de saída. Você pede a uma ferramenta de IA (Claude Code, Codex, Copilot...) e informa os dados da seção 4; ela segue o [SKILL.md](SKILL.md).

A skill é **híbrida e majoritariamente determinística**: tabelas de decisão, um script que gera o esqueleto, um verificador de arquitetura offline, um teste ArchUnit pronto e exemplos que compilam.

## 1. Para que serve

| Faz | Não faz |
|---|---|
| **create**: cria um módulo novo (opcionalmente a partir de uma operação do contrato OpenAPI) | Criar o projeto (use `java-spring-scaffold`) |
| **migrate**: move uma feature de uma estrutura em camadas (`controllers/services/entity/repository`) para o modelo, **sem mudar comportamento** | Escrever ou alterar o contrato OpenAPI (use `openapi-rest-contract`) |
| **review**: audita um módulo contra as regras e relata achados | Mudar código no modo revisão, a menos que você peça |
| Gera esqueleto, aplica as regras, escreve testes por camada, roda o verificador e o build | Inventar endpoints ou regras de negócio que o contrato não define |

## 2. O modelo em uma tela

```
<base>/
  <feature>/
    api/              Controller, Request/Response (records)                       <- adaptador de entrada
    application/
      port/           portas de saída (interfaces): Repository, Gateway, Publisher
      usecase/        <Verbo><Substantivo>UseCase, 1 classe por operação (@Transactional)
    domain/           entidade com regra, value objects, enum, exceções            <- Java puro, sem subpasta
    infrastructure/
      persistence/    <X>Entity (@Entity), <X>JpaRepository, <X>RepositoryAdapter (Entity <-> Domain)
      client/         Feign/REST de APIs externas (implementa uma porta)
      messaging/      Kafka/outbox (implementa uma porta)
  shared/             erros base, handler global, Clock, segurança (o mínimo)
```

Subpacote só quando o pai passa de 7 a 10 classes **e** há um critério claro de agrupamento (por subdomínio, nunca por tipo técnico).

Regras: dependências sempre para dentro; `domain` sem Spring/JPA/Jackson/Lombok/SLF4J; `@Entity`/`@Table` só em `infrastructure`; um `UseCase` por operação, sem interface; `@Transactional` no caso de uso; módulo CRUD sem regra pode usar a entidade JPA direto (pacote plano). Detalhes: [references/architecture.md](references/architecture.md) e [references/decision-guide.md](references/decision-guide.md).

## 3. Pré-requisitos

- Projeto Java Spring Boot com `pom.xml` e Maven Wrapper; JDK 17+ (a skill usa recursos de Java 17: `record`).
- **Node.js 20.19+ ou 22.12+** para os scripts (`scaffold-module.mjs`, `check-architecture.mjs`, `inspect-contract.mjs`). Para o inspetor de contrato rode `npm ci` na pasta da skill uma vez.
- Para `create` a partir de contrato: o `openapi.yaml` do projeto (a skill não cria contrato).
- Para `migrate`: **suíte de testes verde antes de começar** (a rede de segurança).
- Dependência de teste `com.tngtech.archunit:archunit-junit5` (o script do esqueleto avisa e mostra o trecho do `pom.xml`).

## 4. Informações que você deve passar

| Informação | Obrigatória | Observação |
|---|---|---|
| Modo | Sim | `criar` (padrão), `migrar` ou `revisar` |
| Pasta do projeto | Se a IA não estiver aberta nela | A que contém o `pom.xml` |
| Feature | **Sim** | Ex.: `order`, `account`. Se faltar, a IA **pergunta**; nunca escolhe por você. Em `migrar`: as classes/pacotes antigos que formam a feature |
| Escopo no contrato | Se houver contrato | `operationId`, tag ou `GET /caminho` |
| Estilo (`rich` ou `crud`) | Não | A IA decide pela [tabela D1](references/decision-guide.md) e pergunta só se ficar ambíguo |
| Regras de negócio e persistência | Quando o contrato não as deixa claras | A IA pergunta só o que bloqueia |

## 5. Como acionar

```text
Use a skill java-spring-create-module em modo criar.
- Projeto: C:\projetos\meu-monorepo\api-x
- Feature: order (contrato: tag "orders")
- Regras: um pedido novo pode ser pago ou cancelado; pago não cancela
Rode o verificador e o build ao final e me mostre o resultado.
```

```text
Use a skill java-spring-create-module em modo migrar na feature "account" do projeto
C:\projetos\meu-monorepo\api-x. Mostre primeiro a tabela de classes (antes -> depois) e espere minha aprovação.
```

```text
Use a skill java-spring-create-module em modo revisar no módulo "order". Só relate os achados.
```

Claude Code: pasta da skill em `~/.claude/skills/java-spring-create-module`; abra a ferramenta na pasta do projeto. Outras ferramentas: peça que leiam o `SKILL.md` e sigam as instruções (precisam editar arquivos e rodar comandos).

## 6. Os scripts (determinísticos)

```bash
# esqueleto de um módulo (recusa sobrescrever; --dry-run mostra o plano)
node scripts/scaffold-module.mjs --project <pasta> --base-package com.acme.app --feature order [--entity Order] [--style rich|crud] [--no-web] [--dry-run]

# verificador das regras A01-A11 (código de saída 1 se houver violação)
node scripts/check-architecture.mjs --src <pasta>/src/main/java --base-package com.acme.app [--legacy controllers,services,entity] [--strict] [--json]

# lista operações do contrato (opcional)
node scripts/inspect-contract.mjs --entry <openapi.yaml> [--resource <seletor>]

# testes dos próprios scripts
node --test scripts/test/scaffold.test.mjs scripts/test/check-architecture.test.mjs
```

## 7. Exemplos que compilam

[examples/shop](examples/shop/README.md) é um mini-projeto Maven completo: a feature **`order`** (rich: domínio com regras, porta, casos de uso, adapter JPA, controller, testes por camada) e a feature **`category`** (crud plano), mais o `shared` e o `ArchitectureTest`. `mvn test` dentro da pasta: 46 testes verdes (JDK 17 e 21). O esqueleto gerado por `scaffold-module.mjs` também compila e passa (verificado). O `ArchitectureTest.java.tpl` está em [templates](templates).

## 8. O que esperar como resultado

Módulo (ou feature migrada) com testes, verificador em 0 violações, build verde e um relatório com: arquivos, decisões D1-D6, operações cobertas, verificações executadas ou não, pendências. Módulo com `TODO(domain)` sobrando ou comportamento em placeholder **não** é dado como concluído.

## 9. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| A IA pergunta qual feature | Esperado: informe a feature (ou, em `migrar`, as classes antigas) |
| `scaffold-module` recusa criar | Já existe arquivo no destino; escolha outro nome ou migre o existente |
| Verificador acusa A01 em `domain` | Há `@Entity`, Lombok, Spring ou SLF4J no domínio: mova para `infrastructure.persistence` ou reescreva em Java puro |
| Verificador acusa A05 | Uma feature importa o `infrastructure`/`api` de outra: use o caso de uso/`domain` da outra (D6) |
| `ArchitectureTest` não compila | Falta `archunit-junit5` no `pom.xml` (o script mostra o trecho) |
| Mudou o comportamento depois de migrar | Um movimento foi misturado com mudança de lógica: volte ao commit anterior e separe |
