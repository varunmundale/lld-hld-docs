# Using Temporal — an invoice approval and payment workflow

[← Durable Execution HLD](../../../../../../docs/hld/36-durable-execution-engine.md) · [All docs](../../../../../../docs/README.md)

---

Real [Temporal Java SDK](https://github.com/temporalio/sdk-java) code, not a reimplementation.
One business process — validate an invoice, match the PO, get a human to approve it above a
threshold, charge, settle, post to the ledger — carried through every failure mode the
[HLD](../../../../../../docs/hld/36-durable-execution-engine.md) discusses: retries, a lost ack,
a human who never answers, a saga that has to unwind, and a code change made while executions
are in flight.

The point is to see the guarantee hold, and then see what it costs.

## Run it — no server needed

`TemporalDemo` runs against Temporal's in-memory **time-skipping test server**. Same history,
same replay, same retry and timer semantics, plus a virtual clock — so a 72-hour approval
deadline and a two-day settlement window elapse in milliseconds.

```sh
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.TemporalDemo
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.TemporalDemo -Dexec.args=lost-ack
```

Each scenario prints the activity trace, the full event history as a table, and how much time
the execution actually spanned versus how long the demo took.

| Scenario | Setup | What it shows |
|---|---|---|
| `happy` | $2,500, under threshold | straight through; a 2-day `Workflow.sleep` costs one timer row |
| `retry` | ERP fails 2 attempts | attempts 1–3 in history, **no retry loop in the workflow code** |
| `lost-ack` | gateway charges, ack is lost | activity runs **twice**, the pinned key means **one** charge |
| `approve` | human answers after 1h | the signal is in history before the handler runs |
| `reject` | human rejects after 2h | compensation is the next line of ordinary code; no money moves |
| `escalate` | silence for 72h, then approval at 80h | a deadline is a `false` return from `Workflow.await`, not an exception |
| `parked` | nobody ever answers | ends deliberately in `PARKED`; 4 days elapsed, 28ms of wall clock |
| `compensate` | ledger period closed after the charge | `Saga` unwinds — `refundPayment` runs, then the workflow fails |

Actual numbers from a full run:

```
  SCENARIO: escalate  -  TIMEOUT, ESCALATION, THEN APPROVAL
  virtual time elapsed   5d 8h    (72h deadline + escalation + answer at 80h)
  wall clock             38ms
  gateway real charges   1   <-- exactly once
```

## Run it against a real server

For the web UI, a real database, and a real `kill -9`:

```sh
temporal server start-dev                       # https://temporal.io/cli — web UI on :8233

mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceWorker
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceCli -Dexec.args="start INV-1001 1250000"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceCli -Dexec.args="status INV-1001"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceCli -Dexec.args="approve INV-1001"
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceCli -Dexec.args="wait INV-1001"
```

### The crash you should actually run

```sh
# 1. worker that halts hard, right after the gateway charges and before Temporal is told
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceWorker -Dcrash.after=chargePayment
# 2. start an invoice; the worker dies mid-activity
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceCli -Dexec.args="start INV-2001 250000"
# 3. restart the worker normally
mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceWorker
# 4. cat gateway-ledger.txt  ->  ONE charge
```

Nothing in the workflow code handles that. The activity's `StartToClose` timeout expires, the
server re-dispatches, the workflow replays to exactly where it was, and the idempotency key —
recorded in history, not regenerated — makes the gateway reject the second attempt.

Then break it: swap `Workflow.randomUUID()` for `java.util.UUID.randomUUID()` in
[`InvoiceWorkflowImpl`](InvoiceWorkflowImpl.java), repeat, and `gateway-ledger.txt` will have
**two** charges. That one-line difference is the whole of "at-least-once execution,
exactly-once effect."

## The files

| File | What to look at |
|---|---|
| [`InvoiceWorkflow.java`](InvoiceWorkflow.java) | `@WorkflowMethod` vs `@SignalMethod` vs `@QueryMethod`, and why the distinction matters |
| [`InvoiceWorkflowImpl.java`](InvoiceWorkflowImpl.java) | the business process as sequential code — and the determinism rules it obeys |
| [`InvoiceActivities.java`](InvoiceActivities.java) | the boundary: the only code allowed to touch the world |
| [`InvoiceActivitiesImpl.java`](InvoiceActivitiesImpl.java) | retryable vs non-retryable failures, `getInfo().getAttempt()`, heartbeats, failure injection |
| [`InvoiceWorker.java`](InvoiceWorker.java) | workers **poll**; the server never calls you |
| [`InvoiceCli.java`](InvoiceCli.java) | `workflowId` as the client-side dedup key; start / signal / query / cancel vs terminate |
| [`ReplayCheck.java`](ReplayCheck.java) | replay a production history against current code — the versioning safety net |
| [`TemporalDemo.java`](TemporalDemo.java) | the scenarios, on the time-skipping test server |
| [`PaymentGateway.java`](PaymentGateway.java) | a fake PSP that honours an idempotency key, file-backed so it survives a crash |

## The five things worth stealing for an interview

**1. The determinism boundary.** Workflow code may not call `System.currentTimeMillis()`,
`new Random()`, `UUID.randomUUID()`, `Thread.sleep()`, or do any I/O. It calls
`Workflow.currentTimeMillis()`, `Workflow.newRandom()`, `Workflow.randomUUID()`,
`Workflow.sleep()` — all recorded, all identical on replay — and everything else goes in an
activity. In an agent design this is the line that decides where the **LLM call** goes: it is
an activity, never workflow code.

**2. The idempotency key is a workflow-level decision.**

```java
String idempotencyKey = "idem-" + Workflow.randomUUID();   // recorded once, reused forever
ChargeResult charge = payments.chargePayment(idempotencyKey, invoice);
```

Stable across retries *and* across replay. Derived inside the activity, it would be neither.

**3. Retry policy belongs to the caller, not the callee.** The activity throws honestly; the
stub carries `RetryOptions` — initial interval, backoff coefficient, max attempts, and
`setDoNotRetry("InvalidInvoice")`, because a 400 is not a blip. Note the two stubs in
`InvoiceWorkflowImpl`: fast reads and money movement get different timeouts, different attempt
counts, and in production different task queues.

**4. Human-in-the-loop is four lines.**

```java
boolean answered = Workflow.await(Duration.ofHours(72), () -> decision != null);
if (!answered) { activities.escalateToController(invoice, "72h"); ... }
```

No polling job, no `pending_approvals` table, no held thread. The wait is server-side; the
worker can be redeployed or scaled to zero during it.

**5. Versioning is the tax, and `getVersion` is how you pay it.**

```java
int v = Workflow.getVersion("add-fraud-check", Workflow.DEFAULT_VERSION, 1);
if (v >= 1) { activities.fraudCheck(invoice); }
```

Run any scenario and look for `MARKER_RECORDED  marker 'Version'` in the printed history —
that marker is what pins an old execution to the old branch forever. Without the guard,
inserting that call would make every in-flight execution fail replay and **stick**. The branch
is now permanent archaeology until the last old run drains, which is exactly the cost the HLD
describes.

## What this example does not cover

Each of these is a section in the [HLD](../../../../../../docs/hld/36-durable-execution-engine.md):
child workflows, `continue-as-new` and history-size limits, cancellation scopes, custom search
attributes and the visibility index, per-namespace quotas and multi-tenancy, worker build-id
versioning, cross-region replication, and running the server yourself (Cassandra, compaction,
retention, archival — the part that is actually your on-call).

The `postToLedger` activity here also cheats in a way worth naming out loud: a real ledger is a
double-entry store with ACID transactions, and the workflow **coordinates** it rather than
holding balances. Event history is not a book of record.

## Related

- [36. Durable Execution Engine (Temporal-style)](../../../../../../docs/hld/36-durable-execution-engine.md)
  — the design, the [pitfalls](../../../../../../docs/hld/36-durable-execution-engine.md#pitfalls),
  and [where it fits](../../../../../../docs/hld/36-durable-execution-engine.md#where-it-fits--four-systems-four-verdicts)
- [17. Order Management System](../../../../../../docs/lld/17-order-management-system.md) — the
  same class of problem solved by hand, with an explicit state machine
- [29. Payment System](../../../../../../docs/hld/29-payment-system.md) — idempotency and
  exactly-once effects
