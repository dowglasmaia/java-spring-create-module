# Architecture: "Hexagonal Pragmática"

A simplified hexagon organized by feature. It takes **ports and adapters** from Hexagonal (domain isolated), **the dependency rule and explicit use cases** from Clean Architecture, **rich entities and ubiquitous language** from lightweight DDD, and **small cohesive classes** from SOLID. It deliberately cuts: a DTO layer everywhere, mappers for everything, an interface for everything, and the "domain entity vs JPA entity" split when it does not pay its cost.

## Layers and dependencies

```
                    ┌──────────────────────────────────────────┐
  HTTP ───────────► │  api  (inbound adapter)                  │
  Messaging ──────► │  Controller, request/response, mappers   │
                    │            │ calls                       │
                    │            ▼                             │
                    │  ┌──────────────────────────────────┐    │
                    │  │          APPLICATION             │    │
                    │  │  usecase/  <Verb><Noun>UseCase   │    │
                    │  │  (1 class per operation,         │    │
                    │  │   @Transactional, execute())     │    │
                    │  │  port/     OUTPUT PORTS          │    │
                    │  │            │ uses                │    │
                    │  │            ▼                     │    │
                    │  │  ┌────────────────────────────┐  │    │
                    │  │  │          DOMAIN            │  │    │
                    │  │  │  Entity, value objects,    │  │    │
                    │  │  │  enums, rules, exceptions  │  │    │
                    │  │  │  (pure Java)               │  │    │
                    │  │  └────────────────────────────┘  │    │
                    │  └──────────────────────────────────┘    │
                    │            ▲ implements the ports        │
                    │            │                             │
  Database ◄─────── │  infrastructure (outbound adapters)      │
  External APIs ◄── │  persistence / client / messaging /      │
                    │  security / seed                         │
                    └──────────────────────────────────────────┘

  Arrows always point inward. Domain knows nobody.
```

## Package structure

```
com.acme.app
├── order                              ← one feature = one cohesive module
│   ├── api                            inbound adapter (flat; add response/ only when it grows)
│   │   ├── OrderController
│   │   ├── CreateOrderRequest / OrderResponse     (records, only where the contract needs them)
│   │   └── OrderWebMapper             (only when the mapping is long, e.g. generated OpenAPI DTOs)
│   ├── application
│   │   ├── port                       OUTPUT PORTS (interfaces only)
│   │   │   └── OrderRepository
│   │   └── usecase                    1 class per operation
│   │       ├── CreateOrderUseCase
│   │       ├── PayOrderUseCase
│   │       ├── CancelOrderUseCase
│   │       └── GetOrderUseCase
│   ├── domain                         pure Java, no sub-package while it is readable at a glance
│   │   ├── Order                      entity with business rules
│   │   ├── OrderItem                  value object
│   │   ├── OrderStatus                enum
│   │   ├── OrderNotFoundException
│   │   └── InvalidOrderException
│   └── infrastructure                 outbound adapters
│       └── persistence
│           ├── OrderEntity            @Entity (an infrastructure class)
│           ├── OrderJpaRepository     Spring Data
│           └── OrderRepositoryAdapter implements OrderRepository, converts Entity <-> Domain
├── category                           ← CRUD feature: flat, JPA entity used directly (decision D1)
└── shared                             ← minimal: error base classes, error handler, Clock, security
```

`infrastructure` only has the sub-packages the feature needs: `persistence`, `client` (Feign/REST), `messaging` (Kafka/outbox), `security` (caller identity), `seed` (demo data).

## When to create a sub-package

Only when the parent package passes what is readable at a glance (about 7 to 10 classes) **and** there is a clear grouping criterion. Before that, flat is easier to navigate. This holds for `usecase/` too: 4 use cases stay flat; with 20 you group **by sub-domain** (`usecase/transfer`, `usecase/statement`), never by technical type. The checker warns (W04) when a package holds more than 12 classes.

## Flow of a request

```
Controller ──► PayOrderUseCase ──► Order (rule)
                    │
                    └──► OrderRepository (interface in application.port)
                                  ▲
                                  │ implements
                         OrderRepositoryAdapter ──► OrderJpaRepository ──► DB
                                  │
                           converts Entity <-> Domain
```

## Key decisions

1. **One use case class per operation.** `<Verb><Noun>UseCase`, concrete (no interface), `@Service`, not `final`, one public method `execute`. Cohesion comes from the package, not from a god class. A use case may call another use case when it truly composes it (same transaction, `REQUIRED`).
2. **Domain is 100% framework-free.** No `@Entity`, `@Table`, `@Column`, `@Autowired`, Jackson, Lombok or SLF4J. Pure Java. Any class annotated for JPA/JDBC/ORM is infrastructure and lives in `infrastructure.persistence`. Non-negotiable.
3. **Output ports live in `application.port`**, are interfaces only, and are named by need (`OrderRepository`, `PixGateway`, `RechargeEventPublisher`, `CallerIdentity`). Records and enums that a port exchanges belong in `domain`.
4. **Mapping only at the boundaries.** The `RepositoryAdapter` converts `Entity <-> Domain` (highest cost of the model, and what protects the domain); the `api` converts request/response/generated DTOs <-> domain.
5. **`api` talks to use cases and domain types, never to the infrastructure.** When an OpenAPI generator already produces DTOs and an `<Tag>Api` interface, the controller implements it and the DTOs stay in `api`. Never expose a domain or JPA class through the API.
6. **A port only where inversion is real.** Interfaces only for *outputs* that change with infrastructure. No interface for use cases or controllers.
7. **Transaction in the use case.** `@Transactional` on the use case class or method (Spring stays in `application` only for `@Service` and `@Transactional`).

## What each model contributed

| Model | Taken | Dropped |
|---|---|---|
| Hexagonal | Ports and adapters, isolated domain, inbound (`api`) vs outbound (`infrastructure`) adapters | Entry port as an interface for everything |
| Clean Architecture | Dependency rule, one use case per operation | Input/Output boundary objects |
| DDD | Rich entities, value objects, ubiquitous language | Complex aggregates, factories, domain events everywhere |
| Classic layered | Simplicity, familiarity | Top-down dependency all the way to the database |

## Trade-offs

- **Entity vs Domain duplication** is the price of framework independence. A module that is pure CRUD without business rules may use the JPA entity directly (flat module, rule 11). Do not force purity where there is no domain to protect.
- **More small classes** is the price of one use case per operation; the gain is an obvious transaction boundary and a test per operation. Keep them short; shared logic goes to the domain.
- **Package-by-feature** makes extracting a microservice easier and avoids a `service` package with 80 classes.
- **Protect the rules with an architecture test** (ArchUnit) so the discipline does not depend on code review.

## Enforced rules (the checker and `ArchitectureTest` implement exactly these)

| Id | Rule |
|---|---|
| A01 | `..domain..` imports no `org.springframework`, `jakarta.*`, `javax.*`, `com.fasterxml`, `org.hibernate`, `lombok`, `org.slf4j`, `org.apache.kafka`, `feign` |
| A02 | `..domain..` does not depend on `..application..`, `..api..` or `..infrastructure..` |
| A03 | `..application..` does not depend on `..api..`, `..infrastructure..`, `jakarta.persistence`, `jakarta.servlet`, `org.springframework.web`, `org.springframework.http`, `org.springframework.data`, `org.springframework.jdbc`, `org.springframework.kafka`, `org.springframework.cloud` |
| A04 | `..api..` does not depend on `..infrastructure..` |
| A05 | A feature does not use another feature's `infrastructure` or `api` |
| A06 | `..application.port..` holds interfaces only; `..application.usecase..` declares no interfaces |
| A07 | `@RestController`, `@Controller`, `@RestControllerAdvice`, `@Repository`, `@Entity`, `@Table` are not in `..domain..` or `..application..`; `@Repository`, `@Entity`, `@Table` are not in `..api..` (ORM annotations live only in `..infrastructure..`) |
| A08 | `@Transactional` is not in `..domain..` or `..api..` |
| A09 | No `@Autowired` anywhere (constructor injection) |
| A10 | `..shared..` depends on no feature |
| A11 | In a rich feature (one that has `domain/`), `@Entity` lives only in `infrastructure.persistence` (checker only) |
| A12 | Classes in `..application.usecase..` are named `<Verb><Noun>UseCase`; a `*UseCase` class lives only there |
