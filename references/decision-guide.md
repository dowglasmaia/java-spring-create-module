# Decision guide (apply in order, write the result before coding)

## D1 - rich or crud?

Answer the four questions about the feature. **Any "yes" -> `rich`.** All "no" -> `crud`.

| Question | Examples that are "yes" |
|---|---|
| State transitions? | order NEW -> PAID -> CANCELLED; Pix PENDING -> COMPLETED/FAILED |
| Invariant across fields or across aggregate parts? | order needs >= 1 item; amount > 0 and <= balance |
| A calculation or a rule beyond bean validation? | total of items; fee; balance debit |
| An outbound call or event (payment gateway, bank, Kafka)? | MaiaBank call; recharge event |

`crud` examples: categories, tags, countries, settings lists. A feature starts `crud` and becomes `rich` the day a "yes" appears: then **migrate** it (create `domain`, split the entity), do not bolt rules onto the flat module.

## D2 - port or not?

| Dependency | Port in `domain`? | Name |
|---|---|---|
| Database access of a rich feature | yes | `<Entity>Repository` |
| External API / bank / payment | yes | `<Thing>Gateway` |
| Event publishing | yes | `<Event>Publisher` |
| E-mail / SMS / push | yes | `<Channel>Sender` |
| Clock | no | inject `java.time.Clock` |
| The feature's own service | **no** | the controller uses the class |
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

## D4 - split the service?

Split `<Feature>Service` only if one is true: more than ~250 lines; two method groups share no collaborator; one group needs a different transaction model (for example Pix: reserve -> external call -> confirm, never inside one transaction). Name the new class by capability (`PixService`, `StatementService`), keep both in `application`. Never `*UseCase`, never an interface.

## D5 - web contract

| Situation | Controller |
|---|---|
| OpenAPI-generated interface exists | `implements <Tag>Api` (not `final`), maps generated DTOs <-> domain at the boundary, inside `infra.web` |
| No contract / hand-written endpoint | `@RestController` + `record` request/response in `infra.web` |
| Trivial contract (primitives only) | no request/response class: parameters directly |

Controllers: no logic, no logging, no transactions, no repositories.

## D6 - two features meet

| Need | Do |
|---|---|
| A needs data/behaviour of B | call B's `application` service, use B's `domain` types |
| A needs a different shape of B | declare a port in A's `domain` (`<B>Lookup`), implement it in A's `infra` by calling B's service |
| A needs B's tables | never join across features in SQL from A's infra without a stated reason; ask |

## D7 - exceptions

| Failure | Class | HTTP (via `GlobalExceptionHandler`) |
|---|---|---|
| Business rule broken | feature exception extending `shared.error.BusinessRuleException` | 422 `{code:"UNPROCESSABLE_ENTITY", message}` |
| Not found | feature exception extending `shared.error.NotFoundException` | 404 `{code:"NOT_FOUND", message}` |
| Input shape (bean validation) | `MethodArgumentNotValidException` in web | 422 (or the project's rule) |
| Unexpected | generic handler | 500 `{code:"INTERNAL_SERVER_ERROR", message}` without internals |

Existing error messages are part of the contract: when migrating, keep text and status identical.

## D8 - where does cross-cutting code go?

`shared` is the bottom of the model: it may depend on **no feature** (rule A10). Anything that needs a feature's classes belongs to that feature.

| Code | Place |
|---|---|
| Global exception handler, `ApiError`, error base classes | `shared.web`, `shared.error` |
| `Clock`, jackson/other generic beans | `shared.config` |
| Security **infrastructure** (JWT decoder, validators, CORS, entry points) | `shared.security`; it receives anything feature-specific as a bean of a framework type (for example an `AuthorizationManager`) that a feature provides - `shared` never imports the feature |
| Resolving the caller into the feature's own model (who is the account holder) | the owning feature: a port in its `domain` (`CallerIdentity`) + adapter in `<feature>.infra.security`, used by its `application` service |
| Seed / demo data (profile `local`) | the owning feature's `infra.seed`, guarded by `@Profile` |
| Outbox + relay + Kafka producer of one feature | that feature's `infra.messaging` |
| A pure helper used by one feature | that feature (`domain` if it is a business rule, `infra` otherwise) |

Keep `shared` small: if a class is used by one feature, it belongs to that feature.
