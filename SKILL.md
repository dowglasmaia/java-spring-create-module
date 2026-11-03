---
name: java-spring-create-module
description: Create, migrate or review a feature module in a Java Spring Boot project using the "Hexagonal Pragmática" model - package-by-feature, framework-free domain at the center, one use case class per operation, ports only for outputs, api and infrastructure as adapters. Hybrid and mostly deterministic (decision tables, a scaffold script, an architecture checker, ArchUnit template, compiled examples). Modes - create (new module, optionally from an OpenAPI operation), migrate (move a layered feature into the model, behaviour preserved), review. Ask which feature/resource when the request omits it. Do not use to start a new project or author an API contract.
---

# Java Spring - feature modules (Hexagonal Pragmática)

One architecture, applied the same way every time. **Domain pure in the center, one `<Verb><Noun>UseCase` class per operation orchestrating, ports only for outputs, everything else is an adapter: `api` (inbound) and `infrastructure` (outbound: JPA, clients, messaging).** Details and diagrams: [architecture](references/architecture.md). Worked, compiled code: [examples/shop](examples/shop/README.md) (`order` = rich, `category` = crud, 46 tests green on JDK 17 and 21). Every choice below is a table or a rule - when a rule applies, apply it; do not improvise.

## 0. Head: what the developer must provide

| Input | Required | Notes |
|---|---|---|
| **Mode** | yes | `create`, `migrate` or `review` (default `create`; if the request says refactor/move/restructure = `migrate`) |
| **Project folder** | yes | the folder with `pom.xml`; ask if the tool is open on a monorepo root |
| **Base package** | yes | read it from the main `@SpringBootApplication` class; do not ask if it can be read |
| **Feature** | yes | the business capability (`order`, `account`, `transaction`). `create`: ask if absent. `migrate`: the legacy classes/packages to move. Never pick one for the developer |
| **Scope** | create: if a contract exists | `operationId`, tag, or `METHOD /path` from the OpenAPI file. No contract = ask for it; do not invent endpoints |
| **Style** | no | `rich` or `crud`. Decided by the [decision guide](references/decision-guide.md) D1; ask only if D1 is ambiguous |
| Business rules / persistence semantics | only if blocking | ask the one question that blocks, continue independent work meanwhile |

## 1. The model in 12 rules (non-negotiable)

1. **Package by feature.** `<base>.<feature>.{api,application,domain,infrastructure}` plus a minimal `<base>.shared`. Never a top-level `controller`, `service`, `repository`, `entity` package.
2. **Dependencies point inward:** `api -> application -> domain` and `infrastructure -> application -> domain`. `domain` imports nobody (only `java.*`, its own package, `shared.error`).
3. **`domain` is 100% framework-free:** no Spring, no JPA/`jakarta.*`, no Jackson, no Lombok, no SLF4J. Entities carry the business rules (state transitions, invariants) and are built through static factories (`create`, `restore`); no public setters. ORM annotations (`@Entity`, `@Table`) live only in `infrastructure`.
4. **Output ports live in `application.port`**, interfaces only, and exist only where infrastructure can vary: repository, payment/bank gateway, e-mail, event publisher, caller identity. Named by need (`OrderRepository`, `PixGateway`, `RechargeEventPublisher`).
5. **One use case class per operation** in `application.usecase`: `<Verb><Noun>UseCase`, concrete (no interface), `@Service`, not `final`, public method `execute`. No `*Service`, no `*Impl`.
6. **Cohesion comes from the package.** A use case may inject another use case it composes; shared business logic goes to the domain, never to a base class. Group `usecase/` by sub-domain only when it is hard to read (about 10 or more classes), never by technical type.
7. **`@Transactional` on the use case** (class or method). The `api`, the domain and the adapters are not transactional.
8. **Persistence is infrastructure:** `@Entity` classes, Spring Data repositories and the `<Entity>RepositoryAdapter` live in `infrastructure.persistence`. The adapter implements the port and is the only place that maps `Entity <-> Domain`.
9. **Web is an inbound adapter in `api`:** controller, request/response `record`s and generated OpenAPI DTOs live there and never leave it. Never expose a domain or JPA class. Controllers hold no business logic and depend on use cases, never on `infrastructure`.
10. **Clients and messaging are infrastructure:** `infrastructure.client` (Feign/REST), `infrastructure.messaging` (Kafka/outbox) implement ports.
11. **CRUD exception (flat module):** a feature with no business rule (D1) uses the JPA entity directly in one flat package, with a `<Entity>Service`. Everything else still applies (constructor injection, `@Transactional`, web records, tests).
12. **The architecture is enforced by tests:** an `ArchitectureTest` (ArchUnit) in the build and `scripts/check-architecture.mjs` for quick/offline checks.

## 2. Fixed layout

```
<base>/
  <feature>/                         (rich)
    api/               <Entity>Controller, <Entity>Request, <Entity>Response (+ <Entity>WebMapper when the mapping is long)
    application/
      port/            <Entity>Repository, <Thing>Gateway, <Event>Publisher, CallerIdentity   (interfaces only)
      usecase/         Create<Entity>UseCase, Get<Entity>UseCase, Pay<Entity>UseCase ...        (1 class per operation)
    domain/            <Entity>, <Value>, <Status>, feature exceptions                          (flat, pure Java)
    infrastructure/
      persistence/     <Entity>Entity, <Entity>JpaRepository, <Entity>RepositoryAdapter
      client/          <Name>FeignClient / <Name>GatewayAdapter       (only if the feature calls an external API)
      messaging/       <Name>PublisherAdapter, outbox classes          (only if the feature publishes events)
      security/        caller-identity adapter                         (only if the feature resolves the caller)
      seed/            demo data, @Profile("local")                     (only if the feature has seed data)
  <feature>/                         (crud: flat)
                       <Entity> (@Entity), <Entity>Repository (Spring Data), <Entity>Service, <Entity>Controller, <Entity>Request, <Entity>Response
  shared/
    error/             BusinessRuleException (422), NotFoundException (404)        (plain Java; domain may use them)
    web/               ApiError {code, message}, GlobalExceptionHandler
    config/            Clock bean, generic cross-cutting beans
    security/          JWT decoder/validators/CORS (no feature imports; see decision D8)
```

Names: `<Entity>` PascalCase; `<Entity>Entity` only for the JPA class of a rich feature; ports end with the role (`Repository`, `Gateway`, `Publisher`); adapters end with `Adapter`; use cases end with `UseCase`. Folders that would be empty are not created, and a sub-package is created only when its parent passes 7 to 10 classes (D9).

## 3. Decision procedure

Run these in order for every module. Full tables with examples: [decision guide](references/decision-guide.md).

- **D1 rich vs crud:** the feature is `crud` only if it has *no* state transitions, *no* cross-field invariants, *no* calculation and *no* outbound call. Otherwise `rich`.
- **D2 port or not:** create a port (in `application.port`) only for an *output* whose implementation varies by infrastructure. Do not create ports for use cases, mappers or helpers.
- **D3 mapping:** hand-written `toDomain`/`toEntity` (private static) in the adapter by default. MapStruct only if the project already uses it and the mapping has more than ~8 fields. Web mapping = `static from(...)`/`toDomain()` on the records.
- **D4 use cases:** one class per operation, `<Verb><Noun>UseCase`, `execute`. Compose by injecting another use case; a flow that must not be one transaction (reserve -> external call -> confirm) is orchestrated with `TransactionTemplate` inside its use case.
- **D5 web contract:** if an OpenAPI-generated interface exists, the controller implements it and maps generated DTOs at the boundary, inside `api`. Otherwise use `record`s in `api`.
- **D6 cross-feature:** feature A may use feature B's `application.usecase` classes and `domain` types; never B's `infrastructure` or `api`. If A needs a B capability with a different shape, define a port in A's `application.port` and adapt B behind it.
- **D9 sub-packages:** none until the parent has about 7 to 10 classes and a clear grouping criterion.

## 4. Workflow - `create`

1. Locate project, build tool, base package, existing modules, test base classes, OpenAPI entry (if any). Read [contract-first](references/contract-first.md) when a contract exists; the optional `node scripts/inspect-contract.mjs --entry <openapi.yaml> [--resource <selector>]` lists operations.
2. Resolve the feature and scope (ask if missing). Apply D1-D6 and write the decisions in one short block before coding.
3. **Scaffold** (deterministic): `node <skill>/scripts/scaffold-module.mjs --project <dir> --base-package <pkg> --feature <name> [--entity <Name>] [--style rich|crud] [--no-web] [--dry-run]`. It refuses to overwrite, creates only the files the style needs, ensures the `shared` kit and the `ArchitectureTest`, and reports missing `pom.xml` dependencies. Add those dependencies.
4. Replace the `TODO(domain)` markers with the real model, **tests first** for every rule (domain test red -> implement). Follow the compiled [order example](examples/shop/src/main/java/com/acme/shop/order) (use cases, ports, adapters) for rich and the [category example](examples/shop/src/main/java/com/acme/shop/category) for crud.
5. Wire the contract (D5), add the business-event log in the service ([logging](references/logging.md)).
6. Verify ([verification](references/verification.md)): `node <skill>/scripts/check-architecture.mjs --src <src/main/java> --base-package <pkg>` must report 0 violations, then the project's Maven verify including `ArchitectureTest`.
7. Report: files, decisions D1-D6, operations covered, checks run/not run, open questions.

## 5. Workflow - `migrate` (layered -> feature, behaviour preserved)

Read [migration](references/migration.md) first. Summary: **safety net green before any move; one feature at a time; each step compiles and passes the full suite; use `git mv` so history follows; no behaviour change in the same commit as a move.** Order inside a feature: (1) inventory and class mapping table approved by the developer, (2) create `domain` with entity + exceptions by extracting rules from services/entities, and the ports in `application.port`, (3) split JPA entity from domain entity and add the adapter in `infrastructure.persistence`, (4) turn each service operation into a `<Verb><Noun>UseCase` in `application.usecase` (delete the service interface and `*Impl`), (5) move controller/DTOs to `api`, (6) move clients/messaging to `infrastructure`, (7) delete the emptied legacy packages, (8) tighten the checker from `--legacy` to strict.

## 6. Mode `review`

Run `check-architecture.mjs`, then walk [the checklist](references/verification.md#review-checklist). Report findings with `file:line`; change nothing unless asked.

## 7. Tests required per layer

| Layer | Test | Rule |
|---|---|---|
| domain | plain JUnit + AssertJ, no Spring | every invariant and transition: valid and invalid |
| application | one test per use case, with an in-memory fake of the port (or Mockito) and a fixed `Clock` | each outcome and each domain exception propagating |
| infrastructure.persistence | `@DataJpaTest` (project's DB strategy) on the adapter | round trip entity <-> domain, queries |
| api | `@WebMvcTest` with the use cases mocked and `GlobalExceptionHandler` imported | status, body, validation, error mapping |
| architecture | `ArchitectureTest` | once per project |

## 8. Stop and ask

A required input is missing (section 0) - D1 is ambiguous - a business rule or persistence semantics are not derivable - the feature needs a change of the OpenAPI contract - a class cannot be classified by the migration table - moving a class would change behaviour or a public contract - a new dependency is needed beyond ArchUnit/validation.

## 9. Do not

Create a `*Service` or `*Impl` in `application` - add an interface for a use case - create a sub-package before the parent is crowded - put `@Entity`, `@Table` or Spring annotations in `domain` - return JPA or domain objects from a controller - log in controllers, domain or mappers - catch-and-rethrow - create empty placeholder folders - edit generated sources - mix a package move with a behaviour change.

The [OpenAPI REST contract skill](../openapi-rest-contract/SKILL.md) authors contracts (outside this skill). `java-modern-engineering` covers code-level Java; this skill owns **where code lives and how it depends**.
