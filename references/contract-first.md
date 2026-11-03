# Contract-first implementation

## Read the project's shape

- Locate the real entry document, commonly `src/main/resources/openapi/openapi.yaml` (or `swagger/*.yaml`). Multi-file descriptions use `$ref` relative to the referring file. Use `scripts/inspect-contract.mjs` to validate and list operations; keep the source YAML as the authority.
- Search `pom.xml` for the generator (`openapi-generator-maven-plugin` or `swagger-codegen-maven-plugin`), `inputSpec`, `interfaceOnly`, generated package names and the phase that runs it. Inspect the generated interfaces and DTOs after `generate-sources`. **Never modify generated sources.** If the project has no generator, implement the REST boundary directly from the contract with records; do not introduce the plugin just to satisfy this reference.
- Inspect a nearby completed feature (its `api`, `application`, `domain`, `infrastructure.persistence`, tests) and follow it.

## Map the selected operations

1. Match the selection to exact `operationId`, tag/resource or `METHOD /path`. A tag can intentionally select several related operations. If matches cross unrelated domains, ask which to implement.
2. Record method, path, parameters, request body, response status/body/headers, security and shared schemas for every selected operation. Do not treat a request field as mandatory only because it appears in an example. Do not merge request and response types when the contract distinguishes them.
3. If a generated interface exists, the controller (`api`) implements it with exactly its signatures. **Generated DTOs stay inside `api`**: the controller maps them to domain/service inputs and back. Do not create duplicate API interfaces or DTOs. When one generated interface covers several features (one tag, one controller), the controller lives in the feature that owns the tag and calls the other features' services (D6).
4. Orchestration goes to one `<Verb><Noun>UseCase` per operation (concrete class, no interface), business invariants to framework-free `domain`, technology to `infrastructure`. Constructor injection. Add a port only for an output (D2) and a mapper only where D3 says so.
5. Follow the project's actual persistence choice. Do not silently treat a temporary map as durable storage or return fabricated success from unimplemented work. If persistence, authorization or business outcomes are essential yet unspecified, ask one focused question before implementing those parts.
6. Do not change the OpenAPI contract unless explicitly requested (contract authoring belongs to the contract skill).

## Boundaries

Extend existing components rather than introducing duplicate controllers, beans or routes. Limit changes outside the selected feature to necessary wiring, tests and build adjustments, and explain them.
