# Contract-first implementation

## Read the project's shape

- Locate the real entry document, commonly `src/main/resources/openapi/openapi.yaml`. Multi-file descriptions use `$ref` relative to the referring file. Use `scripts/inspect-contract.mjs` to validate and bundle for inspection; keep the source YAML as the authority.
- Search `pom.xml` for `openapi-generator-maven-plugin`, `inputSpec`, `generatorName`, `interfaceOnly`, generated package names, and the phase that runs it. Inspect existing generated interfaces and DTOs after `mvnw generate-sources` (or the project's configured phase). Never modify generated sources. If the project does not use OpenAPI Generator, implement its established REST boundary directly from the contract; do not introduce the plugin merely to satisfy this reference shape.
- Inspect a nearby completed module, its `package-info.java`, controller, use case class, domain model, output port/adapter, exception handling, and tests. Follow that layout. A project created by `java-spring-scaffold` places Spring Modulith modules as sibling packages under the application base package, with `api`, `application`, `domain`, and `infrastructure` inside each. Other projects may organize these differently.

## Map the selected operations

1. Match the developer's selection to exact `operationId`, tag/resource, or HTTP method plus path. A tag can intentionally select several related operations. If matches cross unrelated domains, ask which operations to implement.
2. Record method, path, parameters, request body, response status/body/headers, security, and shared schemas for every selected operation. Do not infer a request field as mandatory solely because it appears in an example. Do not merge request and response DTOs when the contract distinguishes them.
3. If generated interfaces exist, implement the corresponding interface with exactly its signatures and generated DTOs. Keep generated DTOs at the REST boundary; map to domain/application inputs when the types or responsibilities differ. Do not create duplicate API interfaces or DTOs.
4. Put orchestration in application use case classes (one concrete `<Verb><Noun>UseCase` per operation, no interface), business invariants in framework-free domain code, and technology concerns in infrastructure or the established API adapter. Inject collaborators through constructors. Add a port or mapper only when it decouples a concrete dependency or conversion.
5. Follow the project's actual persistence choice. Do not silently treat a temporary map as durable storage or return fabricated success from unimplemented work. If persistence, authorization, or business outcomes are essential yet unspecified, ask a focused question before implementing those parts.

## Preserve boundaries

For Spring Modulith, keep internal packages inside the owning business module and use exported APIs or events for cross-module collaboration. Extend existing components rather than introducing duplicate controllers, beans, or routes. Limit changes outside the selected module to necessary wiring, tests, and build adjustments, and explain those changes.
