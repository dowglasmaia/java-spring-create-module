# Architecture: "Hexagonal Pragmática"

A simplified hexagon organized by feature. It takes **ports and adapters** from Hexagonal (domain isolated), **the dependency rule and explicit use cases** from Clean Architecture, **rich entities and ubiquitous language** from lightweight DDD, and **small cohesive classes** from SOLID. It deliberately cuts: a DTO layer everywhere, mappers for everything, an interface for everything, one `UseCase` class per operation when the service is simple, and the "domain entity vs JPA entity" split when it does not pay its cost.

## Layers and dependencies

```
                    ┌──────────────────────────────────────────┐
                    │            INFRA / ADAPTERS              │
                    │                                          │
  HTTP ───────────► │  infra.web                               │
  Messaging ──────► │  (Controller, Listener, request/response)│
                    │            │                             │
                    │            ▼ calls                       │
                    │  ┌──────────────────────────────────┐    │
                    │  │          APPLICATION             │    │
                    │  │  <Feature>Service                │    │
                    │  │  (orchestrates, @Transactional)  │    │
                    │  │            │                     │    │
                    │  │            ▼ uses                │    │
                    │  │  ┌────────────────────────────┐  │    │
                    │  │  │          DOMAIN            │  │    │
                    │  │  │  Entity, ValueObject,      │  │    │
                    │  │  │  rules, exceptions,        │  │    │
                    │  │  │  OUTPUT PORTS (interfaces) │  │    │
                    │  │  └────────────────────────────┘  │    │
                    │  └──────────────────────────────────┘    │
                    │            ▲ implements                  │
                    │            │                             │
                    │  infra.persistence                       │
  Database ◄─────── │  (JPA Entity, Spring Data, Adapter)      │
  External APIs ◄── │  infra.client / infra.messaging          │
                    └──────────────────────────────────────────┘

  Arrows always point inward. Domain knows nobody.
```

## Package structure

```
com.acme.app
├── order                          ← one feature = one cohesive module
│   ├── domain
│   │   ├── Order                  entity with business rules
│   │   ├── OrderItem              value object
│   │   ├── OrderStatus            enum
│   │   ├── OrderRepository        OUTPUT PORT (interface)
│   │   ├── OrderNotFoundException
│   │   └── InvalidOrderException
│   ├── application
│   │   └── OrderService           use cases: create, pay, cancel ...
│   └── infra
│       ├── web
│       │   ├── OrderController
│       │   └── CreateOrderRequest / OrderResponse     (records, only where the contract needs them)
│       └── persistence
│           ├── OrderEntity        @Entity (an INFRA class)
│           ├── OrderJpaRepository Spring Data
│           └── OrderRepositoryAdapter   implements OrderRepository, converts Entity <-> Domain
├── category                       ← CRUD feature: flat, JPA entity used directly (see decision D1)
└── shared                         ← minimal: error base classes, error handler, Clock, security
```

## Flow of a request

```
Controller ──► OrderService ──► Order (rule)
                    │
                    └──► OrderRepository (interface in domain)
                                  ▲
                                  │ implements
                         OrderRepositoryAdapter ──► OrderJpaRepository ──► DB
                                  │
                           converts Entity <-> Domain
```

## Key decisions

1. **One service per feature, not one `UseCase` class per operation.** `OrderService` groups cohesive use cases. Split only when it passes ~250 lines or gains distinct responsibilities/collaborators. This avoids "hundreds of classes" while keeping SRP reasonable.
2. **Domain is 100% framework-free.** No `@Entity`, `@Column`, `@Autowired`, Jackson, Lombok or SLF4J. Pure Java. Any class annotated for JPA/JDBC/ORM is infra and lives in `infra.persistence`. Non-negotiable.
3. **Mapping only at the persistence boundary.** The `RepositoryAdapter` converts `Entity <-> Domain`. It is the highest cost of the model and what protects the domain. Hand-written by default; MapStruct if the project already uses it and the mapping is large.
4. **Controller: `record`s, only where the contract needs them.** Request/Response are records inside `infra.web`. When an OpenAPI generator already produces DTOs, those stay in `infra.web` too. Never expose a domain or JPA class through the API.
5. **A port only where inversion is real.** Interfaces only for *outputs* that change with infrastructure: repository, payment/bank gateway, e-mail, event publisher. No interface for the service; the controller depends on the concrete class. The "entry port" is the service itself. This removes half the interfaces of a pure hexagon.
6. **Transaction in the application layer.** `@Transactional` on service methods is an accepted pragmatism (Spring stays in `application`, but only for that annotation and `@Service`).

## What each model contributed

| Model | Taken | Dropped |
|---|---|---|
| Hexagonal | Ports and adapters, isolated domain | Entry port as an interface for everything |
| Clean Architecture | Dependency rule, use-case layer | One UseCase per class, Input/Output boundaries |
| DDD | Rich entities, value objects, ubiquitous language | Complex aggregates, factories, domain events everywhere |
| Classic layered | Simplicity, familiarity | Top-down dependency all the way to the database |

## Trade-offs

- **Entity vs Domain duplication** is the price of framework independence. A module that is pure CRUD without business rules may use the JPA entity directly (flat module, rule 11). Do not force purity where there is no domain to protect.
- **Package-by-feature** makes extracting a microservice easier and avoids a `service` package with 80 classes.
- **Protect the rules with an architecture test** (ArchUnit) so the discipline does not depend on code review.

## Enforced rules (the checker and `ArchitectureTest` implement exactly these)

| Id | Rule |
|---|---|
| A01 | `..domain..` imports no `org.springframework`, `jakarta.*`, `javax.*`, `com.fasterxml`, `org.hibernate`, `lombok`, `org.slf4j`, `org.apache.kafka`, `feign` |
| A02 | `..domain..` does not depend on `..application..` or `..infra..` |
| A03 | `..application..` does not depend on `..infra..`, `jakarta.persistence`, `jakarta.servlet`, `org.springframework.web`, `org.springframework.http`, `org.springframework.data`, `org.springframework.jdbc`, `org.springframework.kafka`, `org.springframework.cloud` |
| A04 | `..infra.web..` does not depend on `..infra.persistence..` |
| A05 | A feature does not use another feature's `infra` |
| A06 | `..application..` declares no interfaces |
| A07 | `@RestController`, `@Controller`, `@RestControllerAdvice`, `@Entity`, `@Repository` are not in `..domain..` or `..application..` |
| A08 | `@Transactional` is not in `..domain..` or `..infra.web..` |
| A09 | No `@Autowired` field injection anywhere (constructor injection) |
| A10 | `..shared..` depends on no feature |
| A11 | In a rich feature (one that has `domain/`), `@Entity` lives only in `infra.persistence` |
