# Logging in feature modules

If the project already ships structured logging (for example Spring Boot ECS/JSON over Logback, a `requestId` in the MDC, one access line per HTTP call), follow it. A module adds **business events only**: do not add `logback` files, logging libraries, AOP loggers, helper utilities or per-method entry/exit logs.

## Tracing

Tracing is native and automatic when the project enables it: `traceId`/`spanId` are added to every log line. A module logs nothing about tracing. Create a manual span only for a business operation that matters for diagnosis, never per method, and never put personal or financial data in span attributes.

## Where to log

| Place | Rule |
|---|---|
| `infra.web` (controller, request/response records) | No logging. The access line already records method, path, status and duration. |
| `application` (the `<Feature>Service`) | One `INFO` line when a **state-changing** method succeeds (`created`, `paid`, `cancelled`). Reads log nothing, or `DEBUG` at most. |
| `domain` | **Never** (no SLF4J import is allowed there). Express failures with exceptions. |
| `infra.persistence` | No logging, except an unexpected storage failure translated once into an application exception. |
| `infra.client`, `infra.messaging` | Log only unexpected external failures (`WARN`/`ERROR`) with the cause, once, where they are translated into a domain/application exception. |
| `shared.web` exception handler | The single place that logs failures: business/validation errors at `INFO` without stack trace, unexpected errors at `ERROR` with stack trace. |

Never log and rethrow the same exception: it produces duplicate lines. Log where the exception is handled, not where it passes.

## Shape of a line

```java
private static final Logger log = LoggerFactory.getLogger(OrderService.class);

log.atInfo()
    .addKeyValue("orderId", order.id())
    .addKeyValue("customerId", order.customerId())
    .log("order created");
```

- **Message:** constant, lower case, past-tense event (`order created`, `pix confirmed`). No string concatenation, no ids inside the text, no "Starting.../Finished..." pairs.
- **Context as key-values:** stable `camelCase` names (`orderId`, `customerId`), identical across features so one search follows an entity. `requestId` comes from the MDC automatically; never pass it manually.
- **Levels:** `ERROR` = needs human action; `WARN` = degraded but handled; `INFO` = business event; `DEBUG` = diagnosis, off by default. No `INFO` inside loops or per item.
- **Logger:** `private static final Logger` from SLF4J, one per class. Lombok `@Slf4j` only in classes where the project already uses Lombok **and never in `domain`**.
- **Volume:** one line per business operation; batch operations log one summary line with a count.

## What must never appear

Passwords, tokens, secrets, e-mail, names, Pix keys, CPF, account numbers, descriptions or free text typed by users, monetary amounts, full request/response bodies, query strings, entity or DTO `toString()`. Log opaque ids (the internal id, not the e-mail). When in doubt, log the id and leave the value out.

## Tests

Do not assert on log output. Tests prove behavior. When a module adds the central exception handler, test the response and status, not the log line.
