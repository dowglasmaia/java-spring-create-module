---
name: java-spring-create-module
description: Create or review a business module in an existing Java Spring Boot project from a selected OpenAPI operation or resource, following the project's architecture and generated interfaces (one concrete UseCase class per use case, ports only at real boundaries). Ask which resource to implement when the request omits it. Do not use to start a new project or author an API contract.
---

# Java Spring create module

Create (or review) a module: implement contract-defined behavior in an **existing** Java Spring Boot project. The OpenAPI contract owns the REST interface; the project's current package layout, build configuration, and module boundaries govern the Java implementation.

## Required selection

If the developer has not identified a module, resource, service, tag, `operationId`, or HTTP method plus path, **ask which one they want before generating or changing code**. A suitable question is: “Qual recurso do contrato OpenAPI devo implementar? Pode indicar o módulo/tag, `operationId` ou `GET /caminho`.” A resource name alone is sufficient when it selects a clear set of operations. Inspect the contract read-only to offer concrete choices when that helps, but do not pick the first or only example operation on the developer's behalf. If the selection matches unrelated operations or more than one plausible module, show the candidates and ask which scope they mean.

If no OpenAPI contract exists, ask for its location or for the contract to be supplied; do not invent endpoints or create the contract under this skill. If required business behavior or persistence semantics cannot be derived from the contract and existing project, ask only for those decisions before implementing the dependent behavior. Continue independent inspection while awaiting an answer.

## Module conventions

Follow the existing project first. For projects created by `java-spring-scaffold`, a module is a root package with:

- `api/` — controller implementing the generated interface (not `final`) and a package-private mapper between generated DTOs and domain types.
- `application/usecase/` — **one concrete class per use case**, named `<Verb><Noun>UseCase`, annotated `@Service`, not `final`, with a single `execute` method. No interface per use case and no `Service` suffix. Put `@Transactional` here; take the authenticated owner from the context, never from the client.
- `application/port/out/` — interfaces for what the application needs from outside (repositories, readers). Keep them small and per need.
- `domain/` — framework-free models and rules.
- `infrastructure/` — adapters implementing the output ports.
- Logging: business events only, in the use case class, through SLF4J key-values; none in controllers, domain or mappers. Read [logging](references/logging.md) before writing any log line.
- `package-info.java` with `@ApplicationModule`. Expose to other modules only what must be public, through an interface or `@NamedInterface`; an interface is justified at a module boundary, not inside the module.

## Review mode

When asked to review a module, check against the list above and the contract: one responsibility per use case class, no `Service` + `UseCase` duplication, no generated DTOs in `application` or `domain`, logging follows [logging](references/logging.md) (one clear business-event line per state-changing use case, key-values instead of concatenation, no sensitive data, no log-and-rethrow, no logging in controllers or domain), ports only where they decouple, transaction on the use case, owner filtering in every query, tests for each use case and isolation. Report findings with file references; change code only if asked.

## Workflow

1. Locate the project, its OpenAPI entry document, Maven Wrapper, existing business modules, test conventions, and any OpenAPI Generator configuration. Read [contract-first implementation](references/contract-first.md). The optional `node <skill>/scripts/inspect-contract.mjs --entry <openapi.yaml> [--resource <selector>]` validates and lists operations and their transport shapes; install this skill's pinned Node dependencies with `npm ci` in the skill folder if absent.
2. Resolve the requested scope by path/method, `operationId`, or tag. Inspect referenced request/response schemas, status codes, headers, security declarations, and generated interfaces/DTOs. Where OpenAPI Generator is configured, run the project's configured Maven `generate-sources` phase if generated files are absent. Never edit `target/generated-sources`.
3. Implement only the selected operations using the project's own module structure. Keep domain objects free of Spring and generated DTO types; map transport DTOs at the boundary. Reuse ports and adapters with real responsibilities. Do not add placeholder classes merely to mirror a sample tree. Do not change the OpenAPI contract unless explicitly requested.
4. Add the use case's business-event log line per [logging](references/logging.md); keep the code clean, with no extra helpers or entry/exit logs. Write focused tests for implemented behavior and contract mappings. Run the configured Maven verification, relevant module boundary tests, and startup/HTTP checks when the environment permits. Read [verification](references/verification.md). Fix failures introduced by the change. Report files changed, operations covered, checks passed, and any contract behavior still awaiting a developer decision.

Use the existing [Java Spring Scaffold](../java-spring-scaffold/SKILL.md) only as a reference for projects it created; this skill never recreates a project. The [OpenAPI REST Scaffold](../openapi-rest-contract/SKILL.md) is for authoring contracts and is outside this skill's implementation scope.
