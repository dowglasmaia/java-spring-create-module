# Logging in generated modules

Projects from `java-spring-scaffold` already ship structured JSON logging (Spring Boot ECS over Logback), a `requestId` in the MDC and one `request completed` access line per HTTP call from `shared/logging/RequestLoggingFilter`. A module adds **business events only**: do not add `logback` files, logging libraries, AOP loggers, helper utilities or per-method entry/exit logs. If the project has a different logging setup, follow it.

## Tracing

Tracing is native and automatic: incoming HTTP requests are observed by Spring Boot, and `traceId`/`spanId` are added to every log line. A module logs nothing about tracing. Create a manual span or `@Observed` only for a business operation that matters for diagnosis, never per method, and never put personal or financial data in span attributes. Do not copy trace ids into messages or key-values.

## Where to log

| Layer | Rule |
|---|---|
| `api` (controller, mapper) | No logging. The access line already records method, path, status and duration. |
| `application/usecase` | One `INFO` line when a **state-changing** use case succeeds (`created`, `updated`, `deleted`, `transferred`). Reads log nothing, or `DEBUG` at most. |
| `domain` | Never. Express failures with exceptions. |
| `infrastructure` (adapters) | Log only unexpected external failures (`WARN`/`ERROR`) with the cause, once, where they are translated into an application exception. |
| Exception handler (central) | The single place that logs failures: business/validation errors at `INFO` without stack trace, unexpected errors at `ERROR` with stack trace. |

Never log and rethrow the same exception: it produces duplicate lines. Log where the exception is handled, not where it passes.

## Shape of a line

```java
private static final Logger log = LoggerFactory.getLogger(CreateTransactionUseCase.class);

log.atInfo()
    .addKeyValue("transactionId", transaction.id())
    .addKeyValue("categoryId", transaction.categoryId())
    .log("transaction created");
```

- **Message:** constant, lower case, past-tense event (`transaction created`, `budget threshold crossed`). No string concatenation, no ids inside the text, no "Starting…/Finished…" pairs.
- **Context as key-values:** stable `camelCase` names (`transactionId`, `categoryId`, `ownerId`), identical across modules so a single search follows an entity. `requestId` comes from the MDC automatically; never pass it manually.
- **Levels:** `ERROR` = needs human action; `WARN` = degraded but handled (external failure, rejected state); `INFO` = business event; `DEBUG` = diagnosis, off by default. Do not use `INFO` for loops or per-item work.
- **Logger:** `private static final Logger` from SLF4J, one per class. Use Lombok's `@Slf4j` only if the project already uses Lombok.
- **Volume:** one line per business operation; batch operations log one summary line with a count, not one line per item.

## What must never appear

Passwords, tokens, secrets, e-mail, names, descriptions or free text typed by users, monetary amounts, full request/response bodies, query strings, entity or DTO `toString()`. Log opaque ids (`ownerId` as the internal id, not the e-mail). When in doubt, log the id and leave the value out.

## Tests

Do not assert on log output. Tests prove behavior; the request-correlation behavior is already covered by `RequestLoggingFilterTest`. When a module adds a central exception handler, test the response and status, not the log line.
