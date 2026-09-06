# LLD Problems — Index

*Transcribed from handwritten Xournal++ notebook (`lld_problems.xopp`), 41 pages.
One file per problem.*

**Class diagram** marks the problems that carry a worked class model.
**Stub** means the notebook has the title only — nothing recorded yet.

| # | Problem | Class diagram | Notes |
|---:|---|:---:|---|
| 1 | [Task Scheduler](01-task-scheduler.md) | ✓ |  |
| 2 | [Publisher-Subscriber Model](02-publisher-subscriber-model.md) |  |  |
| 3 | [Payment Wallet (LLD)](03-payment-wallet.md) |  |  |
| 4 | [Rate Limiter](04-rate-limiter.md) |  | — *stub* |
| 5 | [Google Calendar (Slot)](05-google-calendar-slot.md) |  | — *stub* |
| 6 | [Connect Four](06-connect-four.md) | ✓ |  |
| 7 | [Amazon Locker](07-amazon-locker.md) |  |  |
| 8 | [Elevator](08-elevator.md) |  |  |
| 9 | [Parking Lot](09-parking-lot.md) |  |  |
| 10 | [File System](10-file-system.md) | ✓ |  |
| 11 | [Movie Ticket Booking](11-movie-ticket-booking.md) |  |  |
| 12 | [Logging Service](12-logging-service.md) |  |  |
| 13 | [Rate Limiter (Full Design)](13-rate-limiter-full-design.md) |  |  |
| 14 | [Inventory Management](14-inventory-management.md) |  |  |
| 15 | [Build a Rule Engine](15-rule-engine.md) | ✓ |  |
| 16 | [Design Spreadsheet with Formulas](16-spreadsheet-with-formulas.md) | ✓ |  |
| 17 | [Order Management System](17-order-management-system.md) | ✓ | — *added, not from the notebook* |
| 18 | [Metric Collection & Normalization Engine](18-metric-normalization-engine.md) | ✓ | — *added, not from the notebook* |
| 19 | [Autocomplete Index & Ranker](19-autocomplete-index-and-ranker.md) | ✓ | — *added, not from the notebook* |
| 20 | [Inverted Index & Query Evaluator](20-inverted-index-and-query-evaluator.md) | ✓ | — *added, not from the notebook* |

*Problem numbering resets partway through the notebook (pages restart at "1" around
Connect Four, then continue "2, 4, 5, 8, 9, 10..." from Amazon Locker onward) — renumbered
here sequentially 1-16 for continuity, with the original in-notes number preserved as a
parenthetical inside each file where it jumps.*

## Summaries

### 1. [Task Scheduler](01-task-scheduler.md)
Submit one-time and recurring tasks, run them on time, retry up to 3 times, log the
lifecycle. Entities: `Task` / `TaskScheduler` (each split into scheduled vs recurring),
plus an execution environment and a submit API. The core is a time-ordered queue read by a
dispatcher thread that hands work to a separate worker pool.
**Watch for:** the notes' original "poll the PQ every 30 s" loop is the weak point — replace
it with `DelayQueue.take()` so the dispatcher parks until the head is genuinely due.
Never execute in the dispatcher thread, never hold the lock across `execute()`, and never
mutate `nextRunTime` in place while queued. Inject a `Clock` so scheduling is testable.
The file also carries the **1 hr vs 2 hr interview timeboxes** — the 1 hr bar is a working
single-JVM scheduler; the 2 hr bar is that plus one or two extensions taken seriously
(durability, distribution, dependency DAG).

### 2. [Publisher-Subscriber Model](02-publisher-subscriber-model.md)
Write to a topic, read from a topic. NFRs carry the weight: delivery guarantees
(at-least-once is the one to design for, so consumers must dedupe), ordering, thread
safety, retries with a DLQ, and backpressure. Observer pattern — notify every consumer
subscribed to the topic on produce. *Sketch-level notes.*

### 3. [Payment Wallet](03-payment-wallet.md)
Add funds, spend, transfer wallet→wallet, transaction history, optional multi-currency.
Entities: `User`, `Wallet`, `Transaction`, `TransactionStatus`. Everything hangs on
transactional guarantees — atomic, thread-safe, immutable ledger entries, retry on failure.
Named levers: **Saga vs 2PC** for the transfer, and an **idempotency key** so retries do
not double-spend. *Sketch-level notes.*

### 4. [Rate Limiter](04-rate-limiter.md)
Stub — see problem 13 for the worked version.

### 5. [Google Calendar (Slot)](05-google-calendar-slot.md)
Stub — title only.

### 6. [Connect Four](06-connect-four.md)
7×6 board, two players alternate, disc falls to the lowest free cell in a column, game ends
on a 4-in-a-row (vertical, horizontal, diagonal) or a full board. Classes: `Game` (owns
`Board`, two `Player`s, `currentPlayer`, `State`, `winner`), `Board`
(`canPlace`/`placeDisc`/`checkWin`/`isFull`), `Player`, `DiscColor` enum. `checkWin` traces
outward from the placed disc. Invalid moves — full column, out of turn, game already over.
Explicitly out of scope: UI, concurrency, undo, move history, configurable board.

### 7. [Amazon Locker](07-amazon-locker.md)
Driver deposits a package into a size-matched compartment (S/M/L); system generates an
access code with a TTL and notifies the customer; customer enters the code to retrieve.
Entities: `Locker` → `Compartment[]` (`isFree`, `size`, token) and `AccessToken`
(code, expiry, package). Edges: code expiry, 3 failed attempts, all compartments full.
**Extensions:** a sweeper to open/expire stale compartments, indexes by access code and by
(size, isFree), a fallback allocation strategy when the exact size is unavailable, an
`OUT_OF_SERVICE` compartment state, and a `confirmDeposit` API (sensor-driven) so the
system knows the package actually landed.

### 8. [Elevator](08-elevator.md)
3 lifts, 10 floors. `requestLift(fromFloor, direction)` returns a lift via a pluggable
match strategy; the passenger then requests a destination, possibly asynchronously.
No real-time simulation — the system advances on a `step()` trigger.
`LiftService` holds the lifts and does assignment and bounds validation; each `Lift` holds
`currentFloor`, `currentDirection`, up/down queues and a destination queue, and
`step()` drives `onFloorReached` / `changeDirection`.
**Idle state is the point** — do not move a lift with no requests; simplest version parks
idle lifts at the top or bottom floor.
**Extensions:** `LiftType` for express/priority floors, cancel APIs
(`removeLiftCall`/`removeDestCall`), and a lock making the request path a critical section.

### 9. [Parking Lot](09-parking-lot.md)
Vehicle enters → system assigns a size-appropriate slot → ticket issued (start time,
vehicle, slot); on exit the ticket is shown, the fee is computed (rounded to the hour) and
the slot freed. Entities: `Parking`, `ParkingService` (park/unpark), `Slot`, `Ticket`,
`Vehicle`; `step()` for simulation. Edges: lot full, invalid ticket.
**Extensions:** multi-floor (`ParkingLot` → floors → slots, change `findSlot`), a factory
returning a `PricingStrategy` per vehicle type, and a graded concurrency answer —
coarse single lock, then read lock on `findAvailableSlots` + write lock on park/unpark with
retries (OCC-like, low contention).

### 10. [File System](10-file-system.md)
In-memory Windows-Explorer model: create/delete/rename files and folders under one root,
list a folder by path, move a file A→B, de-duplicate names, simple text content.
Entities: `Path` (`List<String>` nodes + `parsePath`), and — the key modelling call — an
abstract **`FileSystemNode`** base with `File` and `Folder` subclasses rather than a
`Folder` whose `addChild` is invalid on files. `FileService` exposes the operations.
**`move` needs cycle detection** (do not move a folder into its own subtree).
**Extensions:** thread safety at three grains — one global lock, per-folder locks acquired
in a consistent order to avoid deadlock, or a read-write lock with retries; and search,
DFS at O(n) or an index if asked to optimise.

### 11. [Movie Ticket Booking](11-movie-ticket-booking.md)
Search a movie or browse theatres → `List<Show>`; pick a show, see available seats, hold
seats for 10 minutes, confirm and get a ticket. Entities: `Theatre`, `Show` (start/end,
seats, movie), `Movie`, plus `SearchService` and `BookingService`
(`selectSeats`/`bookSeats`).
The whole problem is **two users booking the same seat**. Same graded concurrency ladder as
parking: coarse single-threaded booking → read lock on lookups + write lock on the
check-and-set → per-seat locks in a consistent order. A per-seat `AtomicReference` with
`compareAndSet` works only if booking is genuinely one step.
**Extensions:** seat status `{AVAILABLE, ON_HOLD, BOOKED}` with `ON_HOLD → BOOKED` on
payment confirmation, and dynamic addition of shows/movies/theatres.

### 12. [Logging Service](12-logging-service.md)
In-process library: `logger.info(...)` with message, timestamp, severity, multiple levels,
multiple destinations (console, file) and multiple formats (JSON, CSV, plaintext) chosen at
startup. Entities: singleton `Logger`, abstract `Writer` with subclasses, `Formatter`,
`LogRecord`. Format is independent of destination. Concurrency requirement is specifically
**no interleaving within a record**.
**Extensions:** make `log()` non-blocking by putting a bounded blocking queue in front of
flaky writers — which then forces answers on graceful shutdown (drain the queue) and
overflow (drop, or throw to the caller). Note that *async write* and *non-interleaved
write* are two separate properties. Also: hierarchical named loggers
(`com.app.service.payment` falling back to `com.app.service`) via a factory + `getInstance`.

### 13. [Rate Limiter (Full Design)](13-rate-limiter-full-design.md)
`client → rateLimiter → API`: `register(apiName)` with a quota and window, `check(apiName)`
on each call, forward or reject with a code and message. Entities: `Request`,
`RateLimiter` (config + `process(Request)`), `Config`, `SlidingWindow` (a queue that evicts
expired entries), `Map<API, RateLimiter>`, and a `SystemService.step()` for simulated time.
**Extensions:** new algorithms plugged in through the per-API factory; config changes
applied via an `updateConfig` on the live limiter so in-flight window state survives (better
than replacing the object in the factory); concurrency via `ConcurrentHashMap` plus a lock
per limiter key so cleanup-then-admit is atomic; and memory growth handled by extracting
storage and locking into their own layer. Distributed enforcement is out of scope here —
see the [Rate Limiter HLD](../hld/13-rate-limiter.md).

### 14. [Inventory Management](14-inventory-management.md)
Track per-product stock across fixed warehouses: add stock, remove on shipment, transfer
between warehouses, and alert when a product falls below a threshold. Entities:
`WarehouseService` (add/remove/transfer), `Warehouse` (`Map` of product inventory),
`ProductInventory`, `AlertConfig` (threshold + listener). Assumptions: no negative
inventory, fixed warehouse set, order routed only to the nearest warehouse.
Alert configuration is called out as the tricky part.
**Extensions:** preventing overselling — reserve vs available quantities, pessimistic vs
optimistic locking, place-and-ship as one atomic operation; and in-flight transfer stock
modelled as its own (customer-invisible) warehouse.

### 15. [Build a Rule Engine](15-rule-engine.md)
User-defined rules like `age > 18` or `country == "IN"` evaluated against an input object.
A `Condition` is (key, operator, value), and conditions nest — so the model is a
**composite**: abstract `Expression` with `Unary` (constant/key) and `Binary` (`e1 op e2`)
subclasses, letting `(e1 op e2) op (e3 op e4)` compose freely.
Pipeline: tokenize → parse with a stack → collapse the stack and evaluate, with a factory
per operand type (int, String, `instanceOf`). Clarify up front: is the input a map, is
datatype validation in scope, are there brackets, unary operators, nesting.

### 16. [Design Spreadsheet with Formulas](16-spreadsheet-with-formulas.md)
Cells hold a literal or a formula (`= a + b`, operators `+ - * /`, no brackets); updating
one cell re-evaluates every dependent cell, with memoization. API: `setValue(location,
value)` / `getValue(location)`. Model: `Map<Location, Cell>`, each `Cell` holding an
`Expression` — the same composite as the rule engine (`Literal`, `Value`/variable,
`Binary`) — plus a dependency graph kept in both directions (`inDependencies`,
`outDependencies`).
`setValue` parses the input, upserts the dependency edges, **detects a cycle before
committing** (throw otherwise), then walks downstream re-evaluating.

## Recurring themes

Worth reading across the set rather than per problem — these are what the follow-ups
actually probe:

- **Graded concurrency.** Nearly every problem ends with the same ladder: one coarse lock →
  read/write lock split on the check-then-act → fine-grained per-entity locks acquired in a
  consistent order to avoid deadlock. Name the grain you chose and why
  (parking lot, movie booking, file system, rate limiter, elevator, inventory).
- **Check-then-act is the bug.** Seat booking, slot allocation, stock deduction and rate-limit
  admission are all the same race; the fix is to make check and mutate one atomic step.
- **Strategy + factory for the pluggable axis.** Slot allocation, lift assignment, pricing,
  rate-limit algorithm, search — each is a seam where the extension question lands.
- **Composite for anything recursive.** Rule engine expressions, spreadsheet formulas, the
  file system tree and the metric mapping DSL are the same shape — the fourth time it appears,
  it stops being a coincidence and becomes the default answer to "user-defined logic over a record".
- **A `step()` seam instead of wall-clock time.** Elevator, parking lot and rate limiter all
  inject simulated time so behaviour is deterministic and testable — the same point the task
  scheduler makes with an injected `Clock`.
- **Sometimes the ladder is the wrong answer.** The graded lock ladder assumes shared mutable
  state worth protecting. When the read path is hot enough, delete the state instead:
  an immutable artifact behind a volatile reference, swapped atomically
  ([autocomplete index](19-autocomplete-index-and-ranker.md#concurrency-swap-pin-retire),
  [metric mappings](18-metric-normalization-engine.md)). Knowing when to stop refining locks
  is itself the signal.
- **State enough of the lifecycle to answer the edge cases.** `{AVAILABLE, ON_HOLD, BOOKED}`,
  `OUT_OF_SERVICE` compartments, idle lifts, reserved vs available stock — the explicit
  intermediate state is usually the answer to the follow-up.
- **The same five show up in agentic systems too.** Check-then-act, table-driven dispatch,
  an append-only log, a strategy seam, and the explicit intermediate state — mapped onto an
  agent harness and MCP in
  [Local Agent Runtime → the patterns underneath](../hld/37-local-agent-runtime.md#the-lld-patterns-underneath).
- **Say what is out of scope, out loud.** Every worked file has an out-of-scope list; scoping
  is graded, silence is not.

### 17. [Order Management System](17-order-management-system.md)
*Added — not from the notebook.* Drive an order through payment → allocation → fulfilment →
delivery, where every lifecycle input arrives as an **event from a system you don't control**
(payment, inventory, fraud, WMS, carrier), at-least-once and out of order. The design is a
**table-driven state machine**: `Map<(State, EventType), Transition>` with every transition
split into a pure **guard**, an aggregate-only **action**, and an **effect** that returns
commands instead of calling anyone. `Order` has no `setState` — `handle(event)` is the only
way in, and every change appends an immutable `OrderTransition`.
**The load-bearing idea:** an inbound event has **four** outcomes, not two — `APPLIED`,
`IGNORED` (duplicate or already past it — normal, don't alert), `DEFERRED` (valid later, two
systems raced — park and replay), `REJECTED` (genuinely impossible — DLQ). Also: an explicit
`CANCELLING` state because compensation is asynchronous and can fail; a transactional
**outbox** so side effects fire exactly once per transition; dedupe as a unique constraint on
`(source, eventId)`; and ordering by the state machine rather than by `occurredAt`, because
clocks on other people's servers aren't comparable.

### 18. [Metric Collection & Normalization Engine](18-metric-normalization-engine.md)
*Added — not from the notebook.* The inside of the
[Metrics Aggregation Platform](../hld/38-metrics-aggregation-platform.md): pull metrics from
forty third-party providers, turn forty schemas into one, and roll them up — while the same
numbers keep arriving twice, and sometimes different.
**The load-bearing idea:** the mapping is **data, not code**. A `MappingSpec` of
`Expr` nodes (the same composite as the [rule engine](15-rule-engine.md) and
[spreadsheet](16-spreadsheet-with-formulas.md)) is interpreted by one `Normalizer`, so a
mapping fix is a config version rather than a deploy — and because every fact is stamped with
its `mapping_version` and `run_id`, the past can be re-normalized from the stored raw payload.
`Connector` is the only provider-specific class, and it does transport and pagination only;
retries, backoff and rate-limit leases live once in `CollectionRunner`.
Ingest borrows the OMS's four outcomes — `APPLIED`, **`DUPLICATE`** (same key, same value:
the normal case for a trailing re-poll — no write, no dirty window, no alert), `RESTATED`
(same key, different value → supersede and mark the window dirty), `REJECTED` (quarantine,
and do **not** advance the watermark). Ingest never mutates an aggregate; it records a fact
and a debt. Aggregates are **mergeable accumulators** — `MeanAcc(sum, count)`, never a stored
average — so streaming and recompute share one `fold` and cannot drift.
**Watch for:** the rollup cell update is check-then-act; the ladder is striped locks → CAS
with a version → single-writer-per-cell by partition, with the nice escape that a lost CAS
can just fall through to the recompute path. Rate-limit buckets (app / account / endpoint)
are acquired in a fixed order, scarcest first.

### 19. [Autocomplete Index & Ranker](19-autocomplete-index-and-ranker.md)
*Added — not from the notebook.* The serving component of
[Search Autocomplete](../hld/39-search-autocomplete.md): generate candidates for a prefix,
rank them, filter them, and return ten of them inside a 50 ms budget at 200k QPS.
**Three ideas carry it.** (1) The index is an **immutable segment behind a volatile
reference**, not a mutable trie — so the read path takes no lock at all, `topK` is a
build-time artifact rather than a maintained cache, and freshness comes from a small delta
segment unioned at query time (a memtable in front of an SSTable, by another name).
(2) **The deadline is an object**, sliced per stage and passed down, with `orDegrade` making
the fallback explicit in the type — one shared budget, never a timeout per call, because
per-call timeouts sum to a number you never promised. (3) `ServingSnapshot` pins
(index, model, denylist) **once per request**, so a request can never score v7 candidates
with a v8 model.
**Watch for:** this is the problem where the usual concurrency ladder is the *wrong* answer —
copy-on-write beats any lock, and the only real hazard is retiring a segment under a live
reader (`tryPin` must be a CAS that refuses to resurrect zero, and a cold segment must be
warmed before it is published or p99 cliffs on every deploy). Also: candidate sources are
**required vs optional** — a slow personal-history source is dropped, a dead global index is
an error; and post-filters run on the full ranked list with truncation **last**, or one
intent eats half the dropdown. The client half matters as much: debounce, cancel in-flight,
and a monotonic sequence number with a high-water mark, or a slow response for `"cor"` lands
on top of a fast one for `"coron"` — the defect users actually report.

### 20. [Inverted Index & Query Evaluator](20-inverted-index-and-query-evaluator.md)
*Added — not from the notebook.* What Lucene does underneath
[Full-Text Document Search](../hld/40-document-search.md): index documents made of lines,
then answer `"connection refused" AND repo:payments` with **every** matching document.
**The abstraction:** a query does not return a set, it returns a `PostingsIterator` over
ascending doc ids, and every operator — AND, OR, NOT, phrase, filter — is an iterator
composing other iterators. Nothing is materialized, so cost tracks the **rarest** term rather
than the corpus. `advance(target)` over a skip list is what makes that true; conjunction is
the **leapfrog**, and sorting sub-iterators by `cost()` ascending is the difference between
fast and correct-but-useless.
**The consequence, and the reason this problem is not the usual one:** `Collector` owns
`minCompetitiveScore()`, the contract that permits skipping. `TopKCollector` raises it as its
heap fills, so block-max WAND can skip 90 % of the postings; `AllDocsCollector` can never
raise it, so **exhaustive retrieval structurally forbids the fastest execution path**. Running
WAND under an exhaustive collector returns silently incomplete results — the worst failure
mode in the file.
**Watch for:** phrase matching intersects documents *first* and only then decodes positions;
the "must not span a line" requirement is enforced by a **position gap** inserted at index
time, not by a check at query time. Doc ids are segment-local and renumbered by every merge —
never persist or return one. A delete is a bit in `liveDocs`, not a removal. Segments are
immutable, the writer is single-threaded, and readers pin a segment list by refcount so a
merge cannot delete files out from under a running query.

## Related

- [HLD problems](../hld/README.md)
- [Local Agent Runtime + MCP](../hld/37-local-agent-runtime.md) — a file-edit MCP tool end to end:
  schema, JSON-RPC wire trace, the compare-and-swap that makes an in-place edit safe, and the atomic write
- [Metrics Aggregation Platform](../hld/38-metrics-aggregation-platform.md) — the HLD that
  [18](18-metric-normalization-engine.md) is the class model for: raw → facts → rollups, restatement, API budgets
- [Search Autocomplete with Relevance](../hld/39-search-autocomplete.md) — the HLD for
  [19](19-autocomplete-index-and-ranker.md): two-stage ranking, the self-generated training signal, safety
- [Full-Text Document Search (Elasticsearch)](../hld/40-document-search.md) — the HLD for
  [20](20-inverted-index-and-query-evaluator.md): mappings, scatter-gather, deep pagination, sharding
- [LLD appendix](../appendix/lld.md) — concurrency primitives and the task execution engine
