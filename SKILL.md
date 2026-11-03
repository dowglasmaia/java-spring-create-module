---
name: java-spring-create-module
description: Create, migrate or review a feature module in a Java Spring Boot project using the "Hexagonal Pragmática" model - package-by-feature, framework-free domain at the center, one service per feature, ports only for outputs, JPA/web/clients as infra. Hybrid and mostly deterministic (decision tables, a scaffold script, an architecture checker, ArchUnit template, compiled examples). Modes - create (new module, optionally from an OpenAPI operation), migrate (move a layered feature into the model, behaviour preserved), review. Ask which feature/resource when the request omits it. Do not use to start a new project or author an API contract.
---

# Java Spring - feature modules (Hexagonal Pragmática)

One architecture, applied the same way every time. **Domain pure in the center, one service per feature orchestrating, ports only for outputs, everything else (web, JPA, clients, messaging) is replaceable infra.** Details and diagrams: [architecture](references/architecture.md). Worked, compiled code: [examples/shop](examples/shop/README.md) (`order` = rich, `category` = crud, 39 tests green on JDK 17 and 21). Every choice below is a table or a rule - when a rule applies, apply it; do not improvise.

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

1. **Package by feature.** `<base>.<feature>.{domain,application,infra}` plus a minimal `<base>.shared`. Never a top-level `controller`, `service`, `repository`, `entity` package.
2. **Dependencies point inward:** `infra -> application -> domain`. `domain` imports nobody (only `java.*`, its own package, `shared.error`).
3. **`domain` is 100% framework-free:** no Spring, no JPA/`jakarta.*`, no Jackson, no Lombok, no SLF4J. Entities carry the business rules (state transitions, invariants) and are built through static factories (`create`, `restore`); no public setters.
4. **Output ports live in `domain`** and exist only where infrastructure can vary: repository, payment/bank gateway, e-mail, event publisher. Named by need (`OrderRepository`, `PixGateway`, `RechargeEventPublisher`).
5. **No interface for the service and none for use cases.** The inbound "port" is the concrete `<Feature>Service` class; the controller depends on it.
6. **One `@Service` per feature** in `application`, grouping cohesive use cases (`create`, `pay`, `cancel`). Split into several services only by D4 (>~250 lines, or distinct collaborators/responsibilities). Not one class per use case.
7. **`@Transactional` on service methods** (only there). Controllers, domain and adapters are not transactional.
8. **Persistence is infra:** `@Entity` classes, Spring Data repositories and the `<Entity>RepositoryAdapter` live in `infra.persistence`. The adapter implements the domain port and is the only place that maps `Entity <-> Domain`.
9. **Web is infra:** controller, request/response `record`s (and generated OpenAPI DTOs) live in `infra.web` and never leave it. Never expose a domain or JPA class in the API. Controllers hold no business logic.
10. **Clients and messaging are infra:** `infra.client` (Feign/REST), `infra.messaging` (Kafka/outbox) implement domain ports.
11. **CRUD exception (flat module):** a feature with no business rule (D1) uses the JPA entity directly in one flat package. Everything else still applies (constructor injection, `@Transactional`, web records, tests).
12. **The architecture is enforced by tests:** an `ArchitectureTest` (ArchUnit) in the build and `scripts/check-architecture.mjs` for quick/offline checks.

## 2. Fixed layout

```
<base>/
  <feature>/                         (rich)
    domain/            <Entity>, <Value>, <Status>, <Entity>Repository (port), <Thing>Gateway (port), feature exceptions
    application/       <Feature>Service            (+ command/query records only if a signature gets long)
    infra/
      web/             <Entity>Controller, <Entity>Request, <Entity>Response
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

Names: `<Entity>` PascalCase; `<Entity>Entity` only for the JPA class of a rich feature; ports end with the role (`Repository`, `Gateway`, `Publisher`); adapters end with `Adapter`. Folders that would be empty are not created.

## 3. Decision procedure

Run these in order for every module. Full tables with examples: [decision guide](references/decision-guide.md).

- **D1 rich vs crud:** the feature is `crud` only if it has *no* state transitions, *no* cross-field invariants, *no* calculation and *no* outbound call. Otherwise `rich`.
- **D2 port or not:** create a domain port only for an *output* whose implementation varies by infrastructure. Do not create ports for services, mappers or helpers.
- **D3 mapping:** hand-written `toDomain`/`toEntity` (private static) in the adapter by default. MapStruct only if the project already uses it and the mapping has more than ~8 fields. Web mapping = `static from(...)`/`toDomain()` on the records.
- **D4 split the service:** only above ~250 lines or when two groups of methods share no collaborator. Name the split by capability (`PixService`, `StatementService`), never `*UseCase`.
- **D5 web contract:** if an OpenAPI-generated interface exists, the controller implements it and maps generated DTOs at the boundary, inside `infra.web`. Otherwise use `record`s in `infra.web`.
- **D6 cross-feature:** feature A may use feature B's `application` service and `domain` types; never B's `infra`. If A needs a B capability with a different shape, define a port in A's `domain` and adapt B behind it.

## 4. Workflow - `create`

1. Locate project, build tool, base package, existing modules, test base classes, OpenAPI entry (if any). Read [contract-first](references/contract-first.md) when a contract exists; the optional `node scripts/inspect-contract.mjs --entry <openapi.yaml> [--resource <selector>]` lists operations.
2. Resolve the feature and scope (ask if missing). Apply D1-D6 and write the decisions in one short block before coding.
3. **Scaffold** (deterministic): `node <skill>/scripts/scaffold-module.mjs --project <dir> --base-package <pkg> --feature <name> [--entity <Name>] [--style rich|crud] [--no-web] [--dry-run]`. It refuses to overwrite, creates only the files the style needs, ensures the `shared` kit and the `ArchitectureTest`, and reports missing `pom.xml` dependencies. Add those dependencies.
4. Replace the `TODO(domain)` markers with the real model, **tests first** for every rule (domain test red -> implement). Follow the compiled [order example](examples/shop/src/main/java/com/acme/shop/order) for rich and the [category example](examples/shop/src/main/java/com/acme/shop/category) for crud.
5. Wire the contract (D5), add the business-event log in the service ([logging](references/logging.md)).
6. Verify ([verification](references/verification.md)): `node <skill>/scripts/check-architecture.mjs --src <src/main/java> --base-package <pkg>` must report 0 violations, then the project's Maven verify including `ArchitectureTest`.
7. Report: files, decisions D1-D6, operations covered, checks run/not run, open questions.

## 5. Workflow - `migrate` (layered -> feature, behaviour preserved)

Read [migration](references/migration.md) first. Summary: **safety net green before any move; one feature at a time; each step compiles and passes the full suite; use `git mv` so history follows; no behaviour change in the same commit as a move.** Order inside a feature: (1) inventory and class mapping table approved by the developer, (2) create `domain` with entity + port + exceptions by extracting rules from services/entities, (3) split JPA entity from domain entity and add the adapter, (4) move the service to `application` (rename `*ServiceImpl` -> `<Feature>Service`, delete its interface), (5) move controller/DTOs to `infra.web`, (6) move clients/messaging, (7) delete the emptied legacy packages, (8) tighten the checker from `--legacy` to strict.

## 6. Mode `review`

Run `check-architecture.mjs`, then walk [the checklist](references/verification.md#review-checklist). Report findings with `file:line`; change nothing unless asked.

## 7. Tests required per layer

| Layer | Test | Rule |
|---|---|---|
| domain | plain JUnit + AssertJ, no Spring | every invariant and transition: valid and invalid |
| application | service with an in-memory fake of the port (or Mockito) and a fixed `Clock` | each use case outcome and each domain exception propagating |
| infra.persistence | `@DataJpaTest` (project's DB strategy) on the adapter | round trip entity <-> domain, queries |
| infra.web | `@WebMvcTest` with the service mocked and `GlobalExceptionHandler` imported | status, body, validation, error mapping |
| architecture | `ArchitectureTest` | once per project |

## 8. Stop and ask

A required input is missing (section 0) - D1 is ambiguous - a business rule or persistence semantics are not derivable - the feature needs a change of the OpenAPI contract - a class cannot be classified by the migration table - moving a class would change behaviour or a public contract - a new dependency is needed beyond ArchUnit/validation.

## 9. Do not

Create a `*UseCase` class per operation - add an interface for a service - put `@Entity` or Spring annotations in `domain` - return JPA or domain objects from a controller - log in controllers, domain or mappers - catch-and-rethrow - create empty placeholder folders - edit generated sources - mix a package move with a behaviour change.

The [OpenAPI REST contract skill](../openapi-rest-contract/SKILL.md) authors contracts (outside this skill). `java-modern-engineering` covers code-level Java; this skill owns **where code lives and how it depends**.
