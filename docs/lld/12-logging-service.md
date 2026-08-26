# 12. Logging Service

[← LLD index](README.md) · [All docs](../README.md)

---

In-process library
1. `logger.info("user signed in")` — message, timestamp, severity
2. `logger.warn` — S1, S2, S3

## Scoping Requirements
1. Will single application call?
2. What if different services call? (where to stamp svc-name?)
3. What levels?
4. Where will logger write?
   - Console
   - File
   - Multiple destinations
5. Formats? JSON, CSV, plaintext — depends on destination (but independent)
6. Concurrency — one record write should not mix up [with another]

## Requirements
1. Single application, singleton central logger
2. Multiple levels
3. Multiple dest
4. Multiple formats — initialized at startup
5. Concurrency

## Entities
- **Logger**: debug, log, warn, info, error, fatal
- **Writer**: Console, File (abstract with subclasses)
- **Writer**: Format {CSV, JSON, plaintext}
- **Formatter**: format
- **LogRecord**: timestamp, level, message

Singleton + Writer composition + Observer on logger + concurrency

## Extensibility
1. How to make `log()` non-blocking?
   - Flaky writers
   - We can make writes **async**
   - Put a blocking queue (which sleeps when condition [full/empty])
   - Process requests

**Follow-ups**
1. Graceful shutdowns
2. Bounded queue → drop in case queue full (throw exception to client)

- Async-write: non-blocking behavior
- Thread-safe-write: no interleaving

2. Hierarchical named logger?
   - (a) `com.app.service` → Logger
   - (b) `com.app.service.payment` → Logger
   - If (b) is null, fallback to (a). Factory + `getInstance`
