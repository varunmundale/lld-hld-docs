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
- **Composite for anything recursive.** Rule engine expressions, spreadsheet formulas, and
  the file system tree are the same shape.
- **A `step()` seam instead of wall-clock time.** Elevator, parking lot and rate limiter all
  inject simulated time so behaviour is deterministic and testable — the same point the task
  scheduler makes with an injected `Clock`.
- **State enough of the lifecycle to answer the edge cases.** `{AVAILABLE, ON_HOLD, BOOKED}`,
  `OUT_OF_SERVICE` compartments, idle lifts, reserved vs available stock — the explicit
  intermediate state is usually the answer to the follow-up.
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

## Related

- [HLD problems](../hld/README.md)
- [LLD appendix](../appendix/lld.md) — concurrency primitives and the task execution engine
