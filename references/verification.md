# Verification

## Always (create and migrate)

1. **Architecture checker** (offline, deterministic): `node <skill>/scripts/check-architecture.mjs --src <project>/src/main/java --base-package <pkg> [--legacy a,b,c] [--strict]`. Exit 0 required. During a migration pass the legacy packages that are not migrated yet with `--legacy`; the final run has none and uses `--strict`.
2. **ArchUnit `ArchitectureTest`** in the project's test suite: green. The scaffold script installs it; it implements the same rules A01-A11.
3. **Project build** with its own wrapper and JDK: `./mvnw -B verify` (Windows: `.\mvnw.cmd -B verify`). Report the test count and compare with the baseline.
4. **Tests per layer** (SKILL.md section 7) exist for what was created or moved: domain (no Spring), use case (fake port, fixed `Clock`), adapter round trip, controller slice with the exception handler.
5. **Contract** (when an OpenAPI contract exists): check method/path, parameter locations and types, body shape, declared statuses and media types against the source YAML before writing controller tests. If a generator is configured, generation succeeds and the controller implements the generated interface without duplicate DTOs or routes.
6. **Start the application** and exercise the touched endpoints and the health check when the environment supports it (contract-consistent requests; no real external providers, no destructive data operations). Separate environment failures from code failures: command, error, log path.
7. A module is **not complete** while behaviour is a placeholder (`TODO(domain)` left in), a mandatory check fails, or a contract mismatch remains. Report the exact blocker.

## Extra checks for `migrate`

- Same test count (or higher) as the baseline; no test was deleted to get green.
- Same HTTP statuses, JSON bodies and messages (the characterization tests prove it); same Kafka topic and payload bytes; Hibernate `validate` still passes (mapping unchanged); no change in SQL table/column names.
- `git diff --name-status` shows `R` (rename) for moved files; modified files are only those the model required; no behaviour change shares a commit with a move.
- No empty legacy package remains in the last step.

## Review checklist

Report findings with `file:line`.

- [ ] `domain` has no framework import (A01) and no dependency on `application`/`api`/`infrastructure` (A02)
- [ ] Entities enforce their invariants and transitions; no public setters; built through `create`/`restore`
- [ ] Ports exist only for outputs (repository, gateway, publisher, sender); ports are interfaces in `application.port`; no interface for use cases (A06)
- [ ] One `<Verb><Noun>UseCase` class per operation in `application.usecase` (A12); no `*Service`, no `*Impl`; no sub-package before the parent is crowded (W04)
- [ ] `@Transactional` only on use cases (A08); external calls are not inside a transaction when the flow demands it
- [ ] `@Entity`/`@Table` only in `infrastructure.persistence` for rich features (A07, A11); the adapter is the only place that maps Entity <-> Domain
- [ ] Controllers are in `api`, depend on use cases only (A04), contain no business logic or logging, never return domain/JPA classes
- [ ] No feature uses another feature's `infrastructure` or `api` (A05); `shared` depends on no feature (A10)
- [ ] Constructor injection only (A09)
- [ ] Logging follows [logging](logging.md): one business-event line per state-changing use case, key-values, no sensitive data, none in domain/controllers
- [ ] Exceptions: business -> `BusinessRuleException` (422), missing -> `NotFoundException` (404), one handler logs
- [ ] A `crud` module really has no rule (D1); a `rich` module is not just getters (if it is, say so)
- [ ] Tests exist for each layer (SKILL.md section 7) and assert behaviour, not getters
