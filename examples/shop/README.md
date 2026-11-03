# Reference project (compiled and tested)

A mini Spring Boot project that shows the "Hexagonal Pragmática" model end to end. Run it: `mvn test` (JDK 17+). 46 tests, all green on JDK 17 and 21.

| Where | What it shows |
|---|---|
| `order/domain` | **Rich** entity `Order` (state transitions, invariants, `create`/`restore`), value object `OrderItem`, enum, exceptions - pure Java, flat |
| `order/application/port` | the OUTPUT PORT `OrderRepository` (interfaces only) |
| `order/application/usecase` | `CreateOrderUseCase`, `PayOrderUseCase`, `CancelOrderUseCase`, `GetOrderUseCase`: one concrete `@Service` per operation, `execute`, `@Transactional`, business-event log, no interface |
| `order/infrastructure/persistence` | `OrderEntity` (@Entity), `OrderJpaRepository`, `OrderRepositoryAdapter` (the only Entity <-> Domain mapping) |
| `order/api` | `OrderController`, `CreateOrderRequest`/`OrderResponse` records |
| `category/` | **CRUD** flat module: JPA entity used directly, no port, no domain package |
| `shared/` | `BusinessRuleException` (422), `NotFoundException` (404), `GlobalExceptionHandler`, `ApiError`, `Clock` bean |
| `src/test/.../order/domain` | domain tests: plain JUnit, no Spring |
| `src/test/.../order/application/usecase` | one test per use case with an in-memory fake of the port and a fixed Clock |
| `src/test/.../order/infrastructure/persistence` | `@DataJpaTest` adapter round trip |
| `src/test/.../order/api` | `@WebMvcTest` with the use cases mocked and the exception handler imported |
| `src/test/.../order/OrderFlowTest` | create -> pay -> cancel rejected, through every layer |
| `src/test/.../ArchitectureTest` | ArchUnit rules A01-A10 and A12 (generated from `templates/ArchitectureTest.java.tpl`) |

Check it with the offline checker (0 violations expected):

```bash
node ../../scripts/check-architecture.mjs --src src/main/java --base-package com.acme.shop
```

Do not copy this project: generate your module with `scripts/scaffold-module.mjs` and use these files as the reference for what to write in the `TODO(domain)` markers.
