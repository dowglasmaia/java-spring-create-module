# Decision guide (apply in order, write the result before coding)

## D1 - rich or crud?

Answer the four questions about the feature. **Any "yes" -> `rich`.** All "no" -> `crud`.

| Question | Examples that are "yes" |
|---|---|
| State transitions? | order NEW -> PAID -> CANCELLED; Pix PENDING -> COMPLETED/FAILED |
| Invariant across fields or across aggregate parts? | order needs >= 1 item; amount > 0 and <= balance |
| A calculation or a rule beyond bean validation? | total of items; fee; balance debit |
| An outbound call or event (payment gateway, bank, Kafka)? | MaiaBank call; recharge event |

`crud` examples: categories, tags, countries, settings lists. A feature starts `crud` and becomes `rich` the day a "yes" appears: then **migrate** it (create `domain`, split the entity), do not bolt rules onto the flat module. A `crud` module is the only place with a `<Entity>Service` (flat package, no use case classes).

## D2 - port or not?

| Dependency | Port in `application.port`? | Name |
|---|---|---|
| Database access of a rich feature | yes | `<Entity>Repository` |
| External API / bank / payment | yes | `<Thing>Gateway` |
| Event publishing | yes | `<Event>Publisher` |
| E-mail / SMS / push | yes | `<Channel>Sender` |
| Who is calling (token subject) | yes | `CallerIdentity` |
| Clock | no | inject `java.time.Clock` |
| A use case | **no** | the controller uses the class |
| A mapper / validator / helper | **no** | plain class or static method |
| Spring Data repository of a `crud` module | **no** | used directly by the service |

## D3 - mapping

| Situation | Mapping |
|---|---|
| Entity <-> Domain | `private static` `toDomain(Entity)` / `toEntity(Domain)` in `<Entity>RepositoryAdapter` |
| Request -> domain input | `toDomain()` on the request record |
| Domain -> Response | `static from(Domain)` on the response record |
| Generated OpenAPI DTO <-> domain | private methods in the controller (or a package-private `<Entity>WebMapper` if > ~40 lines) |
| MapStruct | only if already configured in the project **and** more than ~8 fields; otherwise hand-written |

Domain objects are rebuilt from storage by a static `restore(...)` factory, never by setters or reflection.

## D4 - which use cases, and how are they named?

One class per **operation** the feature exposes or that another feature needs: `<Verb><Noun>UseCase` with a public `execute(...)` (`CreateOrderUseCase`, `PayOrderUseCase`, `GetCurrentAccountUseCase`). Rules:

| Situation | Do |
|---|---|
| Plain read | `Get<Noun>UseCase` / `List<Noun>sUseCase`, `@Transactional(readOnly = true)` |
| State change | one use case per transition (`Pay`, `Cancel`), `@Transactional` |
| Two operations share more than 3 lines of logic | move the logic to the domain (an intention method), not to a base class or a helper |
| A use case needs another operation | inject that use case (same transaction, `REQUIRED`); never copy its code |
| A flow must NOT be one transaction (reserve -> external call -> confirm, as Pix) | the use case orchestrates with `TransactionTemplate`; the external call stays outside any transaction |
| More than ~10 use cases in the package | group by sub-domain (`usecase/transfer`), never by technical type |

Never an interface for a use case, never `*Service` or `*Impl` in `application`.

## D5 - web contract

| Situation | Controller (in `api`) |
|---|---|
| OpenAPI-generated interface exists | `implements <Tag>Api` (not `final`), maps generated DTOs <-> domain at the boundary |
| No contract / hand-written endpoint | `@RestController` + `record` request/response |
| Trivial contract (primitives only) | no request/response class: parameters directly |

Controllers: no logic, no logging, no transactions, no repositories; they depend on use cases.

## D6 - two features meet

| Need | Do |
|---|---|
| A needs data/behaviour of B | call B's use case (`application.usecase`), use B's `domain` types |
| A needs a different shape of B | declare a port in A's `application.port` (`<B>Lookup`), implement it in A's `infrastructure` by calling B's use case |
| A needs B's tables | never join across features in SQL from A's infrastructure without a stated reason; ask |

## D7 - exceptions

| Failure | Class | HTTP (via `GlobalExceptionHandler`) |
|---|---|---|
| Business rule broken | feature exception extending `shared.error.BusinessRuleException` | 422 `{code:"UNPROCESSABLE_ENTITY", message}` |
| Not found | feature exception extending `shared.error.NotFoundException` | 404 `{code:"NOT_FOUND", message}` |
| Input shape (bean validation) | `MethodArgumentNotValidException` in `api` | 422 (or the project's rule) |
| Unexpected | generic handler | 500 `{code:"INTERNAL_SERVER_ERROR", message}` without internals |

Existing error messages are part of the contract: when migrating, keep text and status identical.

## D8 - where does cross-cutting code go?

`shared` is the bottom of the model: it may depend on **no feature** (rule A10). Anything that needs a feature's classes belongs to that feature.

| Code | Place |
|---|---|
| Global exception handler, `ApiError`, error base classes | `shared.web`, `shared.error` |
| `Clock`, jackson/other generic beans | `shared.config` |
| Security **infrastructure** (JWT decoder, validators, CORS, entry points) | `shared.security`; it receives anything feature-specific as a bean of a framework type (for example an `AuthorizationManager`) that a feature provides - `shared` never imports the feature |
| Resolving the caller into the feature's own model (who is the account holder) | the owning feature: a port in its `application.port` (`CallerIdentity`) + adapter in `<feature>.infrastructure.security`, used by its use cases |
| Seed / demo data (profile `local`) | the owning feature's `infrastructure.seed`, guarded by `@Profile` |
| Outbox + relay + Kafka producer of one feature | that feature's `infrastructure.messaging` |
| A pure helper used by one feature | that feature (`domain` if it is a business rule, `infrastructure` otherwise) |

Keep `shared` small: if a class is used by one feature, it belongs to that feature.

## D9 - sub-packages

Create a sub-package only when the parent holds about 7 to 10 classes **and** there is a clear grouping criterion (a sub-domain, never a technical type). Until then stay flat: `domain/` without `model/`, `usecase/` without sub-folders, `api/` with `response/` only when it has many records.
