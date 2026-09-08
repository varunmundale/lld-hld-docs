# Using Temporal — Uber ride dispatch: request, offer, accept

[← Durable Execution HLD](../../../../../../docs/hld/36-durable-execution-engine.md) · [Uber HLD](../../../../../../docs/hld/23-uber.md) · [All docs](../../../../../../docs/README.md)

---

Real [Temporal Java SDK](https://github.com/temporalio/sdk-java) code, not a reimplementation.
One process — a rider requests a ride, the ride is broadcast to nearby drivers, one accepts,
the card is held, the trip runs, the card is settled — carried through the failure modes that
actually happen at 3am: two drivers accepting the same ride in the same millisecond, a push
whose ack is lost, a payment hold whose ack is lost, a driver stolen by another ride between
their tap and our write, a rider cancelling mid-trip, and a task queue with nobody polling it.

Three questions this example exists to answer, each with a scenario you can run:

| Question | Run this | Section |
|---|---|---|
| How is idempotency guaranteed? | `lost-ack`, `at-least-once` | [Idempotency, in four layers](#idempotency-in-four-layers) |
| What exactly does at-least-once mean? | `lost-ack` | [At-least-once, precisely](#at-least-once-precisely) |
| How do task queues work? | `queues` | [Task queues](#task-queues) |

## Run it — no server needed

`RideDemo` runs against Temporal's in-memory **time-skipping test server**. Same history, same
replay, same retry and timer semantics, plus a virtual clock — a 15-second offer TTL and a
five-minute dispatch deadline elapse in microseconds.

```sh
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideDemo
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideDemo -Dexec.args=race
```

Each scenario prints the activity trace (with the **task queue** each activity ran on), the
event history as a table, and how much time the ride actually spanned versus how long the demo
took.

| Scenario | Setup | What it shows |
|---|---|---|
| `happy` | D-101 accepts after 6s | one broadcast round, losing cards withdrawn, three queues in the trace |
| `decline` | two drivers swipe away | declines are signals; the round ends early when all offers are answered |
| `silent` | nobody answers round 1 | the 15s TTL is a durable timer; round 2 goes to different drivers |
| `race` | two drivers accept at once | one `WON`, one `TOO_LATE` — **and there is no lock in this repo** |
| `stale-accept` | accept lands after the TTL | `EXPIRED`, not a second assignment |
| `at-least-once` | push lands, ack is lost | `pushOffer` runs **twice**, the pinned `offerId` shows **one** card |
| `lost-ack` | hold lands, ack is lost | `authorizeFare` runs **twice**, the pinned key holds **one** card |
| `cross-ride` | another ride steals the driver | the one race Temporal *doesn't* solve; a CAS in the fleet service does |
| `cancel` | rider cancels while en route | the Saga unwinds; the cancellation fee gets its **own** key |
| `cancel-early` | rider cancels while searching | nothing to undo — that falls out of the code, it isn't a special case |
| `declined` | card declined after reserve | `doNotRetry`; the Saga puts the driver back; the workflow fails on purpose |
| `no-drivers` | pickup in the middle of nowhere | radius 2→4→8→16km for 5 minutes, then a deliberate `NO_DRIVERS` |
| `queues` | zero workers on `ride-notifications` | tasks sit in the queue; `SCHEDULE_TO_START` fires |

Real numbers from a full run:

```
  SCENARIO: at-least-once
  push attempts           4     <- activity executions
  offer cards shown       3     <- 1 duplicate push suppressed by offerId
  real money movements    2     <- one hold, one capture

  SCENARIO: silent
  virtual time elapsed    13m 20s     wall clock  94ms

  SCENARIO: no-drivers
  virtual time elapsed    5m 0s       wall clock  111ms
```

## Run it against a real server

For the web UI, a real database, and a real `kill -9`:

```sh
temporal server start-dev                          # https://temporal.io/cli — UI on :8233

# three pools, three deployments; kill any one and watch the others carry on
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args="dispatch sfo"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args=payments
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args=notifications

mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideCli -Dexec.args="request RIDE-1"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideCli -Dexec.args="log RIDE-1"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideCli -Dexec.args="accept RIDE-1 D-101 offer-<id>"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideCli -Dexec.args="arrive RIDE-1"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideCli -Dexec.args="start RIDE-1"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideCli -Dexec.args="end RIDE-1 4390"
```

Run `request RIDE-1` twice. The second is refused by the **server**, not by your code, because
`workflowId` is the `rideId`. That is idempotency layer one and it costs one line.

---

## The workflow, in one screen

The whole business process is ordinary sequential code
([`RideWorkflowImpl`](RideWorkflowImpl.java)):

```java
dispatch.validateRider(req);
fareCents = dispatch.quoteFare(req);

String authKey   = "auth-" + Workflow.randomUUID();   // pinned. in history. survives replay.
String captureKey = "cap-" + Workflow.randomUUID();

String winner = findDriver(req);                       // the broadcast loop, below

Saga saga = new Saga(...);
saga.addCompensation(dispatch::releaseDriver, req.rideId(), winner);
PaymentAuth auth = payments.authorizeFare(authKey, req.riderId(), fareCents);
saga.addCompensation(payments::voidAuth, auth.authId());

Workflow.await(Duration.ofMinutes(15), () -> driverArrived || cancelReason != null);
Workflow.await(Duration.ofMinutes(10), () -> tripStarted   || cancelReason != null);
Workflow.await(Duration.ofHours(4),    () -> tripEnded);

payments.captureFare(captureKey, auth.authId(), actualFareCents);
dispatch.releaseDriver(req.rideId(), winner);
```

and the dispatch loop:

```java
while (cancelReason == null && Workflow.currentTimeMillis() < deadline) {
    List<Driver> candidates = dispatch.findNearbyDrivers(req, radiusKm, tried);
    for (Driver d : batch) {
        String offerId = "offer-" + Workflow.randomUUID();      // pinned per offer
        liveOffers.put(offerId, d.driverId());
        pushes.add(Async.procedure(notifications::pushOffer, offerId, d.driverId(), ...));
    }
    Promise.allOf(pushes).get();

    Workflow.await(OFFER_TTL, () -> acceptedDriverId != null
            || cancelReason != null
            || declinedOffers.containsAll(liveOffers.keySet()));

    ...withdraw the losing cards, clear liveOffers, reserve the winner, or widen the radius
}
```

What is **not** here, and would be in every hand-rolled version: a `ride_state` column, a
`dispatch_offers` table with a cron scanning it for expiry, a Redis lock keyed on `driverId`, an
outbox, a "resume from step N" switch, a retry loop, a dead-letter consumer. The absence of all
of that is the value proposition.

---

## Task queues

**A task queue is just a name.** Not a topic you create, not a partition you size, not a
resource you provision. You invent the string, a worker long-polls it, and the server starts
handing that worker tasks whose queue name matches. Nothing else exists.

What actually happens:

```
worker  ── PollWorkflowTaskQueue(name="ride-dispatch-sfo") ──▶  server
                                                                (request held open ~60s)
worker  ◀────────────────── WorkflowTask ─────────────────────  server
worker  ── RespondWorkflowTaskCompleted(commands=[...]) ──────▶  server
```

Activity tasks work the same way on `PollActivityTaskQueue`. Three consequences fall out of the
arrow pointing the "wrong" way:

**1. Nothing is addressed.** The server never dials a worker. It has no hostname, no port, no
health check for one. So workers can sit behind NAT, on a laptop, in another cloud, in another
language — and a deploy is just "stop polling, start polling". There is no service discovery
and no ingress to the worker fleet.

**2. Backpressure is free and correct.** If every worker on a queue is busy or dead, nobody
polls, and tasks accumulate server-side. Nothing is dropped, and nothing is retried into a
downstream that is already on fire. **Queue depth is your saturation metric.** The `queues`
scenario runs a ride with zero workers on `ride-notifications` and you can watch it:

```
     23  ACTIVITY_TASK_SCHEDULED     PushOffer  on queue 'ride-notifications'
     24  ACTIVITY_TASK_SCHEDULED     PushOffer  on queue 'ride-notifications'
     25  ACTIVITY_TASK_SCHEDULED     PushOffer  on queue 'ride-notifications'
     26  ACTIVITY_TASK_TIMED_OUT     TIMEOUT_TYPE_SCHEDULE_TO_START  <- nobody was polling
```

**3. The queue name is your only routing and isolation knob** — and it is enough. Two names are
two independent pools that cannot starve each other, cannot be deployed together, and cannot
share a bad dependency.

This example uses it three ways ([`TaskQueues`](TaskQueues.java)):

| Queue | Carries | Why it is separate |
|---|---|---|
| `ride-dispatch-<city>` | workflow tasks + matching activities | matching is regional and latency-critical; a bad deploy or a supply outage in SFO leaves NYC untouched |
| `ride-payments` | `authorizeFare`, `captureFare`, `voidAuth` | the PSP is slow (seconds, not ms); a bad payments deploy must not touch dispatch; these workers hold credentials dispatch has no business having |
| `ride-notifications` | `pushOffer`, `withdrawOffer`, `notifyRider` | enormous volume, trivially cheap, high failure rate; scale to hundreds of workers and let it fall behind without delaying a ride |

Routing is expressed in exactly two places:

```java
// the CLIENT picks the workflow's queue, once, at start time — it is fixed for that ride
WorkflowOptions.newBuilder().setTaskQueue(TaskQueues.dispatch(city)).setWorkflowId(rideId)

// the WORKFLOW picks each activity's queue
ActivityOptions.newBuilder().setTaskQueue(TaskQueues.PAYMENTS)...
```

An activity stub with no task queue set **inherits the workflow's queue**. That default is
right most of the time, and the dispatch activities lean on it.

### Sizing and cardinality

Per-**city** queues are fine: task queues are cheap names, cardinality stays in the hundreds.
Per-**rider** or per-**driver** queues are not — you would get millions of mostly-idle queues
and pay for the polls. The rule of thumb: a queue should have enough traffic to keep at least
one worker's poll loop busy, or it is a routing hack, not a queue.

Two `WorkerOptions` knobs that people wrongly size together:

```java
.setMaxConcurrentWorkflowTaskExecutionSize(200)   // short, CPU-only replays
.setMaxConcurrentActivityExecutionSize(100)       // long, I/O-bound
.setMaxTaskQueueActivitiesPerSecond(50)           // per-worker dispatch ceiling
```

A slow activity must never be able to starve workflow tasks — those are on the critical path of
every ride in the pool.

### The three timeouts, and which one to page on

| Timeout | Fires when | Means |
|---|---|---|
| `scheduleToStart` | the task sat in the queue with nobody polling | **capacity**. Never "the code is slow." Page on this one. |
| `startToClose` | one attempt ran too long | the code or its dependency is slow |
| `scheduleToClose` | the whole thing, retries included, took too long | give up already |

The offer push carries `scheduleToStart = 5s` deliberately: the offer card is only live for 15
seconds, so a push delivered later than that is delivering an offer the ride has already moved
past. Failing fast and re-dispatching is strictly better. The payments stub deliberately has
**no** `scheduleToStart` — if the payments pool is down we would rather the ride wait, because
dispatch is already done and a driver is en route.

### Sticky queues (worth knowing, invisible in code)

Each worker also polls a private, per-worker queue. After a worker completes a workflow task it
keeps that execution's state cached, and the server routes the *next* task for that execution
back to the same worker — no replay needed. If that worker dies, the sticky task times out
(seconds) and the execution falls back to the shared queue, where another worker replays from
history. You never write this; it is why the steady state is cheap and why a worker restart
costs a small latency blip rather than a stall.

---

## At-least-once, precisely

**Temporal guarantees at-least-once execution of an activity. Never exactly-once.** Not because
of an implementation shortcut — because it is not achievable. From the server's side these two
worlds are identical:

```
  (a) activity ran, changed the world, then the worker died before reporting
  (b) activity never ran
```

Both look like silence. The only correct response to silence, if the process must complete, is
to try again. Any system that claims exactly-once *execution* is either lying or has moved the
problem into a transaction you cannot see.

What Temporal *does* guarantee exactly-once is the **workflow state transition**: each activity
result is written into history exactly once, and replay produces the same decisions. That is
the useful half. The other half is yours:

> **at-least-once execution + an idempotent effect = exactly-once effect**

`lost-ack` is the whole lesson in one trace. The PSP holds the card, then the ack is lost:

```
[ride-payments] authorizeFare  attempt=1  key=auth-b130d52b-...
  [psp] HELD $44.20 key=auth-b130d52b-... -> auth_d0460350   (real money movements: 1)
  !! authorizeFare attempt 1 DID THE WORK, then the ack was lost -> Temporal will retry it
[ride-payments] authorizeFare  attempt=2  key=auth-b130d52b-...
  [psp] key auth-b130d52b-... already seen -> auth_d0460350, NO second hold

real money movements    2   (one hold, one capture — not three)
```

Note the history: it records **one** `ActivityTaskCompleted`. The retries are the engine's
business; the workflow code contains no retry loop at all, and does not know it happened except
via the `duplicateSuppressed` flag the gateway hands back.

`at-least-once` shows the same shape on a non-money effect — a push notification that buzzes a
real phone. Push attempts 4, cards shown 3.

---

## Idempotency, in four layers

Each layer catches something the layer above cannot.

### 1. `workflowId` — the free one

```java
WorkflowOptions.newBuilder().setWorkflowId(rideId)
```

Two taps of "Request", a retried mobile HTTP call, a duplicated Kafka message: the second
`StartWorkflowExecution` is rejected by the server with `WorkflowExecutionAlreadyStarted`. No
dedupe table, no Redis `SETNX`, no TTL to tune. Pick a business identifier for `workflowId` —
`rideId`, `orderId`, `invoiceId` — never a UUID you mint at the call site, or you have thrown
the guarantee away.

`WorkflowIdReusePolicy` controls what happens after the first one closes: `ALLOW_DUPLICATE`
(default), `ALLOW_DUPLICATE_FAILED_ONLY` (retry only if it failed), or `REJECT_DUPLICATE`.

### 2. Keys minted in the workflow, pinned by history

```java
String authKey = "auth-" + Workflow.randomUUID();   // NOT java.util.UUID.randomUUID()
```

`Workflow.randomUUID()` is deterministic: the value is written into history the first time the
line runs, and **every retry of the activity and every replay after a crash reads the same
value back**. Swap in `java.util.UUID.randomUUID()` and the crash-then-replay path mints a fresh
key — the rider's card is held twice, and your test suite will not catch it, because it only
happens when the worker dies in the two-hundred-millisecond window between the PSP write and
the ack.

Mint the key **before** the first thing that can fail, and give every distinct effect its **own**
key. This example carries three, plus one per offer:

| Key | Effect | Why not shared |
|---|---|---|
| `auth-<uuid>` | hold on the card | — |
| `cap-<uuid>` | settle the hold | reusing the auth key would make the gateway suppress a real capture |
| `fee-<uuid>` | cancellation fee | a fee is not a fare |
| `offer-<uuid>` | one offer card on one phone | it is also the dedupe key on the phone, and the token the driver echoes back on accept |

### 3. The downstream has to honour the key

An idempotency key only works if the receiver enforces it. Real payment APIs do. Your internal
services often do not, and that is where the bodies are buried. Three ways to get it, in
descending order of how much you will enjoy them:

1. the API takes an `Idempotency-Key` header (Stripe, Adyen) — use it
2. a unique constraint on the key column, and catch the duplicate-key violation
3. a conditional write / CAS, which is what [`DriverFleet.reserve`](DriverFleet.java) does

For genuinely unkeyable side effects — "send this email", "click submit on a partner portal",
an LLM tool call with no key — the honest answer is that you cannot get exactly-once, and the
design question becomes *which* duplicate is cheaper: a second email, or a lost one. Decide it
explicitly and write it down.

### 4. The race across workflows — the one Temporal does not solve

Inside one workflow execution, serialization is free (see below). Across two, it is not: two
different ride workflows can offer the same driver in the same millisecond, and no amount of
Temporal makes them see each other. Something outside has to arbitrate. Here it is one atomic
compare-and-set:

```java
String holder = assignments.putIfAbsent(driverId, rideId);   // free -> held by this ride
if (holder == null) return true;
return holder.equals(rideId);      // idempotent: our own retry sees "already ours"
```

In production that is a conditional write in the fleet store, or a per-driver workflow that
owns the driver's state and receives assignment requests as signals. The `cross-ride` scenario
runs it: `acceptOffer` returns `WON`, `reserveDriver` returns `false`, and the dispatch loop
simply carries on to the next driver. The rider never knows.

---

## The race: two drivers, one ride, no lock

Three drivers hold the same broadcast round. Two tap ACCEPT in the same millisecond, from two
phones, through two frontend hosts, in two availability zones. Run `race`:

```
  [t+6s] D-101 and D-102 both tap ACCEPT
            D-101 -> WON       (head to pickup)
            D-102 -> TOO_LATE  (taken by D-101)
```

The handler is the business logic you would write single-threaded, because that is what it is:

```java
public AcceptResult acceptOffer(DriverResponse r) {
    if (cancelReason != null)     return lost(RIDE_CANCELLED, ...);
    if (acceptedDriverId != null) return lost(TOO_LATE, "taken by " + acceptedDriverId);
    if (!r.driverId().equals(liveOffers.get(r.offerId()))) return lost(EXPIRED, ...);
    acceptedDriverId = r.driverId();
    return AcceptResult.won(request, fareCents);
}
```

The server writes both updates into **one** history, in some order, and hands them to **one**
workflow thread, which runs them **one at a time**. The second reads the field the first wrote.
There is no lock to acquire, no CAS to get wrong, no lease to renew, and no lock to leak when a
worker dies mid-critical-section — because there is no critical section. Serialization is a
property of the execution model, not something you built.

Compare what this replaces: a Redis lock on `ride:{id}`, a TTL you have to guess, a fencing
token to stop the zombie holder, a background job to reap stuck locks, and a race in the gap
between "lock acquired" and "state written" that you will find in production, not in review.

### Why accept is an *update* and decline is a *signal*

| | Durable? | Returns a value? | Used here for |
|---|---|---|---|
| `@SignalMethod` | yes | no | decline, rider cancel, arrived, trip started/ended |
| `@UpdateMethod` | yes | **yes** | **accept** |
| `@QueryMethod` | n/a — no history write | yes | rider app polling "where is my driver" |

If accept were a signal, the driver would tap ACCEPT, get an optimistic green screen, and learn
400ms later over some other channel that they lost. An update is a durable request *with a
return value*: the update is written to history before the handler runs (so a worker crash
cannot lose an accept a driver really gave), and the phone still renders the truth on the first
frame.

`@UpdateValidatorMethod` runs **before** the history write, so a rejection there leaves no
trace — right for junk from a stale app build. Note what is deliberately *not* rejected there:
an accept for an expired offer. That is a real event from a real driver; it belongs on the
record, and it gets a real answer (`EXPIRED`). Run `stale-accept`.

Queries are the cheap one and the one people forget: "where is my driver" is asked every two
seconds by millions of riders, and it costs zero history events. Query handlers must not block,
must not call activities, and must not mutate.

---

## Determinism and replay

The workflow method must make the same calls in the same order given the same history.

| No | Yes |
|---|---|
| `System.currentTimeMillis()` | `Workflow.currentTimeMillis()` |
| `UUID.randomUUID()`, `new Random()` | `Workflow.randomUUID()`, `Workflow.newRandom()` |
| `Thread.sleep`, `new Thread` | `Workflow.sleep`, `Async.function` |
| HTTP / JDBC / files / env / config | an activity |
| `HashMap` / `HashSet` iteration | `LinkedHashMap` / `LinkedHashSet` |
| `String.format` with the default locale | `Locale.ROOT`, or do it in an activity |

Break it and the failure does not appear where you wrote it. It appears months later, on a
replay, as a `NonDeterministicException`, on a ride that is now wedged.

Adding a step to a workflow with executions in flight is the same hazard:

```java
int version = Workflow.getVersion("add-safety-rating-check", Workflow.DEFAULT_VERSION, 1);
if (version >= 1) dispatch.checkDriverSafetyRating(winner);
```

`getVersion` writes a marker into history the first time it runs, so rides that started before
the deploy keep taking the old branch forever. The cost is that the branch is permanent
archaeology until every pre-deploy execution drains — trivial for a 20-minute ride, much less
trivial for a 90-day subscription workflow.

[`ReplayCheck`](ReplayCheck.java) is the CI guard: replay a corpus of real histories against the
candidate build before shipping.

```sh
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.ReplayCheck
# captured a completed ride: 128 events
# REPLAY PASSES - this build can safely take over rides that are already in flight
```

---

## What it costs

The honest half, and the half that separates a real answer from a brochure.

- **Every activity is at least two round trips** to the Temporal server — schedule and complete.
  A 5ms local call becomes a ~15ms orchestrated one. Do not make an activity per loop iteration.
- **History is not free.** Each ride here is ~125 events. Long-lived or chatty workflows hit the
  event-count and size limits, and the answer is `continueAsNew` — which resets history and
  hands you a fresh set of "what state do I carry over" bugs.
- **Versioning is the real operational tax.** `getVersion` branches accumulate; each one is a
  permanent `if` until every old execution drains.
- **You now operate a database.** Temporal's persistence (Cassandra/MySQL/Postgres + Elasticsearch
  for visibility) is a stateful system on your critical path. Temporal Cloud sells you out of
  this; self-hosting does not.
- **Debugging moves.** The stack trace is in the history, not in your logs. That is better once
  you learn to read it, and disorienting for a week before that.
- **Not a queue and not a stream processor.** If your problem is "process 10M events with no
  per-entity state machine", this is the wrong tool and an expensive one.

Reach for it when the process is **long-lived, stateful, multi-step, and has to survive
crashes** — which a ride is, and a click-stream is not.

---

## The files

| File | What it is |
|---|---|
| [`RideWorkflow.java`](RideWorkflow.java) | the contract: workflow method, update, signals, queries |
| [`RideWorkflowImpl.java`](RideWorkflowImpl.java) | the business process, and the dispatch loop |
| [`RideActivities.java`](RideActivities.java) | the activity contract, and the at-least-once note |
| [`RideActivitiesImpl.java`](RideActivitiesImpl.java) | implementations plus the fault injection |
| [`TaskQueues.java`](TaskQueues.java) | the three queues and why each one exists |
| [`DriverFleet.java`](DriverFleet.java) | supply service, fleet CAS, and the driver's phone |
| [`PaymentGateway.java`](PaymentGateway.java) | file-backed PSP that honours idempotency keys |
| [`RideDemo.java`](RideDemo.java) | 13 scenarios on the time-skipping test server |
| [`RideWorker.java`](RideWorker.java) | the three worker pools, against a real server |
| [`RideCli.java`](RideCli.java) | request / accept / decline / cancel / arrive / start / end |
| [`ReplayCheck.java`](ReplayCheck.java) | the CI guard for non-deterministic changes |

Fault injection knobs, all `-D` system properties:

```sh
-Dfail.<activity>=N          # throw on attempts 1..N BEFORE doing the work
-Dfail.after.<activity>=N    # do the work, THEN throw — the dangerous one
-Dfail.only.driver=D-101     # scope push failures to one driver
-Dfail.declined=true         # the PSP declines the card, non-retryably
```
