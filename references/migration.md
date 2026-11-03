# Migrating a layered feature into the model (behaviour preserved)

For projects that still have `controllers/`, `services/`, `entity/`, `repository/`, `config/`, `exeptions/`, `utils/` at the top. The goal is the layout of [architecture](architecture.md) **without changing behaviour**.

## Preconditions (stop if missing)

1. The test suite is green **before** the first move; save the count (`Tests run: N`). Without characterization tests of the feature, add them first (a move is only safe with a net).
2. The base package stays the same, so component scanning, `@EntityScan` and Spring Data scanning keep working.
3. A branch exists; each step below ends in a commit that compiles and passes the whole suite.
4. The developer approved the **class mapping table** (step 1).

## Step 1 - inventory and mapping table (no code change)

List every class of the feature and classify it with this table. Present the result and wait for approval.

| Legacy class | Target | Notes |
|---|---|---|
| `controllers.<X>Controller` (implements a generated `<Tag>Api`) | `<feature>.infra.web` | stays the implementer of the generated interface; generated DTOs never leave `infra.web` |
| `controllers.validate.*` | shape checks -> `infra.web`; business validation -> `domain` | a validator that decides a business rule becomes a domain method/exception |
| `services.<X>Service` (interface) | **deleted** | the concrete service is the entry point |
| `services.impl.<X>ServiceImpl` | `<feature>.application.<Feature>Service` | merge cohesive impls; split by D4 (e.g. Pix keeps its own service) |
| `services.client.*` local helpers (account/agency lookups) | inline into the service, or the other feature's service (D6) | no helper classes for one-line lookups |
| `services.client.<ext>.*FeignClient` | `<feature>.infra.client` + a port in `domain` (`<Thing>Gateway`) + adapter | timeouts/circuit breaker stay on the adapter |
| `entity.<X>` (JPA) | split: pure class in `domain` + `<X>Entity` in `infra.persistence` | rules move into the domain class; the entity keeps columns only |
| `entity.<X>Enum` | `domain` | pure enum |
| `repository.<X>Repository` (Spring Data) | `infra.persistence.<X>JpaRepository` + port `<X>Repository` in `domain` + `<X>RepositoryAdapter` | custom `@Query` (e.g. the atomic conditional debit) stays in the JPA repository; the port exposes it as an intention (`tryDebit`) |
| `config.HandlerException` | `shared.web.GlobalExceptionHandler` | same statuses, same bodies, same messages |
| `config.kafka.*` (producer, outbox, relay) | `<feature>.infra.messaging` + port `<Event>Publisher` in `domain` | payload and topic stay byte-identical |
| `config.security.*` | `shared.security` | |
| `config.<seed>.*` | `shared.seed` (profile `local`) | |
| `exeptions.*` | `domain` exceptions of the owning feature extending `shared.error.*` | fix the `exeptions` typo in its own commit |
| `utils.*Validator`, `utils.DateConverter` | business rule -> `domain`; pure utility -> `shared.util` | |
| generated sources (`provider.*`) | untouched | never edit or move |

Any class that does not fit = **Stop and ask**.

## Step 2 - extract the domain (tests first)

1. Write domain unit tests for the rules found in the services (red, because the domain class does not exist yet).
2. Create the pure domain class (`create`/`restore`, intention methods, invariants) and the port; make the tests green. No framework imports (Lombok included: write records/constructors by hand).
3. The old service may call the new domain methods while the old entity still exists: commit.

## Step 3 - split the JPA entity and add the adapter

1. `git mv` the old entity to `infra.persistence/<X>Entity` and keep only columns/mappings; keep `@Table`/column names (Hibernate `validate` proves the mapping is unchanged).
2. Spring Data repository -> `<X>JpaRepository`; create `<X>RepositoryAdapter implements <X>Repository` with private static `toDomain/toEntity` (D3).
3. Point the service at the port. Adapter test (`@DataJpaTest`/Testcontainers): round trip. Commit.

## Step 4 - move the service

`git mv` the impl to `<feature>/application/<Feature>Service`, rename, delete its interface, replace Lombok `@RequiredArgsConstructor` by an explicit constructor if the domain rules require no Lombok (application may keep Lombok only if the project's rule says so; the checker does not forbid it there), keep `@Transactional`. Commit.

## Step 5 - move web and clients

Controller and request/response types to `infra.web`; Feign client + adapter to `infra.client`; Kafka producer/outbox to `infra.messaging`. Commit each.

## Step 6 - clean up and tighten

Delete the emptied legacy packages. Run the checker with `--legacy <remaining top-level legacy packages>` while other features are still pending, and **without** `--legacy` (add `--strict`) when the last feature is done. Add the ArchUnit `ArchitectureTest` (scaffold script) as soon as the first feature is migrated; keep it green for migrated features.

## Tests that use storage directly

Integration-test support classes (for example a base class that seeds accounts through Spring Data) need the JPA entities/repositories. Keep `<X>Entity` and `<X>JpaRepository` `public` **only when such test support needs them** (default is package-private); tests are not analysed by the architecture rules. Do not make them public for production code: production code goes through the port.

## Rules during a move

- `git mv` for every moved file (history follows); a commit never mixes a move with a behaviour change.
- Same HTTP statuses, JSON, messages, SQL names, Kafka payload. A diff in any of them is a bug of the migration.
- Atomicity and transactions stay as they are: an atomic `UPDATE ... WHERE balance >= :amount` stays an atomic update behind a port method (`tryDebit`), not a read-modify-write in the domain; a call to an external service stays outside the database transaction.
- If a class is used by two features, decide by D6; do not duplicate it.
- Update imports with the IDE/compiler, not by hand-editing hundreds of lines blind.
- At the end: `git diff --name-status` shows renames (R) for moved files, modifications only where the model required them.
