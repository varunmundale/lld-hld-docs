# Revenue Recognition — End to End

*Standalone whiteboard design. Not part of the HLD problem set — kept separately because there is no
matching problem in the [HLD problem set](hld/README.md).*

*Detail below sourced from the [Stripe Experience notes](https://docs.google.com/document/d/1F0KVioGjgqMCm_sniEwkoGpbFus42kduqZZGJj_f3n4/edit?tab=t.0).*

> **Worked example:** [Revenue Recognition — worked example](revenue_recognition_walkthrough.md)
> — a Netflix subscription traced through journal → ledger → revenue recognition entries → period
> summary, with Stripe's two period-summary formulas worked through.

![Revenue Recognition — end-to-end pipeline](diagrams/revenue-recognition.png)

<sub>Whiteboard source — [open in Excalidraw](https://excalidraw.com/#json=RzulstEkMllbVQ9jA4rOD,0ZAuh-n8lQKxbVTM_SXBcg) · offline copy: [`diagrams/excalidraw/revenue-recognition.excalidraw`](diagrams/excalidraw/revenue-recognition.excalidraw)</sub>

## Flow

1. **Kafka (Oplog) ← Mongo** — change stream off the Mongo oplog is the entry point of the pipeline.
2. **AF Flink processor** — two stages:
   - *De-duplication + filter* — de-dup done in a **3 min window** to optimise; filter-in only the
     relevant upstreams.
   - *Hydration* — calls **pay-server API** to enrich the event (`financial_entity`).
3. **Kafka** → **SQS** — buffer between the streaming and the worker tier; gives **retries** and
   **horizontal scaling**.
4. **RSL Worker (Java)** — consumes SQS, writes back to **Kafka** as CDC events.
5. **CDC partitions** — hourly / offline partitions feeding the batch layer.
6. **Incremental Spark pipeline** — runs on a **1 hr** cadence, upserts into **Iceberg**.
7. Iceberg fans out into two serving paths:
   - **Spark PinotIngestion → Pinot (Offline) → Dashboard** — sub-second query latency.
     Segment config: `"timeColumnName": "accountingperiod"`, `"segmentPushType": "REFRESH"`,
     `maxNumRecordsPerSegment: 500000` (caps rows per segment, so a fat `(merchant, month)`
     splits into many files). Replacement uses the **M-to-N lineage swap** — an atomic
     `startReplaceSegments` / `endReplaceSegments` over an explicit segment list, so a push
     carrying Sept + Nov leaves Oct untouched. Spark output is partitioned by `accountingperiod`.
   - **DataStory (SQL queries, aggregate) → API Reports**.

## Constraints noted on the board

- All APIs live in **pay-server**.
- Any external system reaching Mongo does so **via the API only** — no direct DB calls.

---

# Layer 1 — Data collector

## Mongo as the source of truth

| Question | Answer |
|---|---|
| Leader based? | ✅ Yes (primary–secondary replica set) |
| Oplog created by Debezium? | ❌ No — MongoDB creates it automatically |
| Debezium's role | Reads the oplog and publishes CDC events |
| Oplog ordered? | ✅ Totally ordered **within a replica set** |
| Global ordering across shards? | ❌ No |
| ACID compliant? | ✅ Yes — multi-document transactions supported |
| Consensus | ✅ Raft-inspired leader election and replication |
| MVCC | ✅ WiredTiger uses MVCC + optimistic concurrency control |
| Reads from secondaries? | ✅ Supported (read preference), but may be **stale** unless the right read concern is used |
| Writes | ✅ Primary only |
| Multi-shard transaction | ✅ Transaction coordinator + **2PC** — all participating shards commit or abort together |
| Global timestamp | ✅ Oplog timestamp; monotonic logical time for causal consistency |

## Upstreams

Revenue recognition is driven by the Stripe billing surface — see
[Stripe revenue recognition docs](https://docs.stripe.com/revenue-recognition):

- [Subscriptions and invoicing](https://docs.stripe.com/revenue-recognition/methodology/subscriptions-and-invoicing)
- [Refunds and disputes](https://docs.stripe.com/revenue-recognition/methodology/refunds-and-disputes)
- [Bank transfers](https://docs.stripe.com/revenue-recognition/methodology/bank-transfers)

## What this layer does

- All oplog events are published to the **same topic** — Mercury, **at-least-once** delivery,
  **72 hour retention**.
- Listens to Mongo oplogs and **filters in only the relevant tables** (invoices, payments, refunds …).
- **De-duplication optimizer** — a **3 minute** de-dup window.
- **Event enrichment framework** pulls every event associated with the entity —
  e.g. `refund → payment → invoice`.
- Writes a **fat `financial_entity` object**, fully enriched, to the Kafka sink
  `financial_entity_stream`.

## Out-of-order events

```
Invoice   ts:100
Payment   ts:105
```

Both are processed and emitted — **de-duplication is keyed on the primary key only**, not on time.
Enrichment is what makes this safe: the hydrating read has to see the complete picture.

| | Approach | Trade-off |
|---|---|---|
| **Solution 1** | API call against Mongo **secondaries** | Replication lag < 50 ms, so after 50 ms all replicas are guaranteed to have complete data. Cheap, but needs the delay. |
| **Solution 2** | Read from the Mongo **primary** | Always complete, no delay — at the cost of primary load. |

---

# Layer 2 — Processor (RSL)

**Kafka + SQS + Java.** The processor reads from the upstream queue `financial_entity_stream`.

## Why Kafka *and* SQS?

- **Kafka** — never lose financial events; retain them for **replay and recovery**.
- **SQS** — distribute processing tasks reliably, with **retries and DLQs**.

SQS deletes the event once it is processed. If there is a bug in processing, that event is gone —
Kafka is what lets the ledger be reconstructed and recovered.

## Why SQS and not Flink?

Each financial entity is **independent** — stateless per-event processing for upstream events.
Because this is an accounting ledger, **appending entries suffices** in most cases.

The exception is mutation: an **invoice VOID** mutates the state of the invoice. If the void lands on
a **closed accounting period**, it creates **reversals and corrections in the open period** so closed
periods stay protected.

### Stream state vs business state

| Stream processing state (Flink state) | Business state (database state) |
|---|---|
| Exists only because of the stream | Outlives the stream |
| 3-minute de-duplication windows | RSL processes Invoice **JE #123** … |
| Watermarks | … and later an **Invoice Void** must update **JE #123** |
| Session windows | |
| Running aggregates, join state, CEP patterns | |

The mutation case is business state, so it belongs in a database behind a worker — not in Flink state.

## Why at-least-once semantics on Kafka?

At-least-once is easier because the system can **bias toward duplication rather than loss**.
Exactly-once would require coordinating processing + state changes + message acknowledgement
atomically. De-duplication on the primary key already absorbs the duplicates.

## Scale

| Metric | Value |
|---|---|
| Peak load | **254 RPS** |
| Average load | **118 RPS** |
| Min load | **54 RPS** |
| Total events / day | **5.4 M** |
| Journal-entry RPS | **474** (peak **1592**) |

| Merchant distribution | Rows |
|---|---|
| Largest merchant (OpenAI) | **2.55 B** — ~15% of all rows |
| 99th percentile merchant | **2 M** |
| Median merchant | **1 900** |
| Active revrec merchants | **53 k** |

Storage: **2 TB input → 9 TB output**.

---

# Layer 3 — Reporting

Consumes the **incremental hourly partitions** and writes **4 stateful reports**. It exposes the
ledger with **corrections and reversals reconciled, and voids filtered out**.

Denormalized reports:

- **Ledger** — transform
- **AR-Aging** — filter
- **Month-summary** — denormalize

> Design question: the reporting entries should be buildable from the **journal layer alone**
> (while handling out-of-order events).

Worked through row by row in the
[end-to-end example](revenue_recognition_walkthrough.md#stage-3--revenue-recognition-entries).

## Why not serve straight out of Pinot?

Pinot is not optimized for complex relational query execution:

- joins are limited
- subqueries often materialize intermediate results
- the optimizer is not comparable to PostgreSQL or Spark
- **no read-modify-write** — only good for upserts
- **no ACID**; Pinot is **eventually consistent**

Real-time aggregation *pulled from the journal* works for a handful of entries (< 100). It does not
work for large, complex aggregations.

### Option: transactional DB + async CDC into Pinot
Correct, but **too costly**.

### Option: Java/Kafka materializer + Pinot realtime ingestion
Works for this use case — the reports are already denormalized, so a materializer can maintain them.

## Lambda architecture in Pinot

Pinot supports lambda (batch + speed layer) natively via **hybrid tables**. The problems:

- **No reconciliation** between offline and realtime tables.
- Every query has to perform an aggregation — you have **moved the merge logic into every query**.
- Unreliable upstream **duplication**, duplicates across Pinot.
- Hard accuracy requirement with **partial updates** — there must be a single source of truth.

## Realtime upsert table in Pinot

Problem: how to populate it in bulk?
Solution: snapshot only **closed/frozen segments for closed accounting periods** — though preferably
**don't push business logic into segment freezing**.

## What the serving layer actually needs

- recoverability + schema evolution
- accuracy
- near-real-time support (freshness)
- low query latency
- not too costly

---

# Storage decision — Iceberg

**Spark (batch) + Iceberg** is one of the primary supported combinations for doing upserts.

```sql
-- report.createOrReplaceTempView("updates")

MERGE INTO finance.report t
USING updates s
   ON t.merchantId       = s.merchantId
  AND t.accountingPeriod = s.accountingPeriod
WHEN MATCHED     THEN UPDATE SET *
WHEN NOT MATCHED THEN INSERT *
```

Stack: **Spark 3.3 + Iceberg 1.3 + Airflow 2** — because it is already in place, and it supports
`MERGE INTO`, snapshots, ACID, time travel, partition evolution and cheap storage. Iceberg also needs
**periodic maintenance jobs** (compaction, snapshot expiry).

Reference: [Upserting data using Spark and Iceberg](https://medium.com/datamindedbe/upserting-data-using-spark-and-iceberg-9e7b957494cf)

## Design comparison

| Design | Recovery | Latency | Cost | Complexity |
|---|---|---|---|---|
| Hourly Spark | ⭐⭐⭐⭐⭐ | ❌ 1 hr | High | Low |
| Pinot-only state | ⭐⭐ | ⭐⭐⭐⭐⭐ | Low | High |
| Streaming materializer + Iceberg + Pinot | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ (5–10 min) | ⭐⭐⭐⭐ | ⭐⭐⭐ |

---

# Serving — Pinot

Pinot is a poor **primary** table for reads and bad at read-modify-write primitives — use Iceberg for
that. Between the two Pinot options:

- A **realtime upsert table** solves freshness but not bulk population.
- Better: a **Pinot offline table** partitioned on **merchant + month**. Largest segment ≈ **15 GB**,
  ~**20 min** write. This works because under normal workload only a small subset of
  (merchant, month) — the **open period** — has to be overwritten.

## Segment replacement — who decides what gets replaced?

**Pinot does not work it out, and you do not hand it a delta.** An offline table has no primary key,
no upsert and no content-based merge — nothing in Pinot looks at a row and concludes "this is
September, swap the September segment". Replacement is driven entirely by **segment names**, which
the *batch job* chooses. Two mechanisms:

**1. Name collision (implicit).** Upload a segment named `X`: if `X` already exists it is replaced in
place, otherwise it is added. Segments absent from the push are left alone. This is the real reason a
job carrying September + November data leaves **October untouched** — not because Pinot inspected the
data, but because the job emitted no segment named like October's. Limitation: strictly 1-to-1, so it
cannot change how many segments a period is made of.

**2. Segment lineage (explicit).** `startReplaceSegments(segmentsFrom, segmentsTo)` → upload →
`endReplaceSegments`. **You supply both lists.** Pinot's contribution is *atomicity*, not the diff:
readers see either the old set or the new set, never a mixture and never a gap. That is what makes
**M-to-N** possible — 3 old segments retired by 5 new ones — which name collision cannot express, and
which matters here because `maxNumRecordsPerSegment` lets the segment count for a
`(merchant, period)` grow or shrink as its row count changes.

### How arbitrary is M-to-N?

Fully arbitrary. `segmentsFrom` and `segmentsTo` are independent lists — no cardinality relationship,
and no requirement that the new segments cover the same time range, partition or row count as the old
ones. Either list may be empty: empty `segmentsFrom` is an atomic bulk **add**, empty `segmentsTo` an
atomic bulk **delete**.

What Pinot validates is narrow:

- every name in `segmentsFrom` must currently exist in that table;
- names in `segmentsTo` must not already exist;
- both lists belong to a single table;
- no other **in-progress** lineage entry may overlap your `segmentsFrom` (`forceCleanup=true` revokes
  a stale one).

What it does **not** validate is correctness. Pinot never checks that `segmentsTo` contains the rows
that were in `segmentsFrom`. Hand it a `segmentsTo` missing half the data and the swap succeeds —
atomically and silently. The guarantee is **atomicity, not conservation**, which is why the rebuild
must read the complete partition back out of Iceberg rather than trusting the swap to protect it.

> **Chunk large swaps.** A lineage entry lives in a single ZooKeeper znode holding both name lists. A
> full whale recovery (~5 100 old + ~5 100 new names) runs to several hundred KB against ZK's 1 MB
> default `jute.maxbuffer` — measure it rather than assume the ceiling, but design around it now:
> issue **one lineage transaction per accounting period** rather than one for the whole merchant.
> That trades global atomicity for per-period atomicity, which is the right boundary for an accounting
> ledger anyway, since the period is already the unit that closes.

### The delta trap

Pushing only the changed rows as a **new** segment name makes Pinot **append** it. Offline tables do
no deduplication, so the old rows and the new rows are both served — silent double-counting in an
accounting ledger.

So the delta's only job is to identify **which** partitions are dirty. What gets pushed is always a
**full rebuild** of those partitions:

1. the hourly Spark run `MERGE INTO`s Iceberg, and therefore knows the set of
   `(merchant, accounting_period)` it touched;
2. for each dirty pair, read the **complete** partition back out of Iceberg — not the delta;
3. build segments from it (record-count split), naming them so the name encodes the partition key;
4. list the table's current segments from the controller and filter by that name prefix →
   `segmentsFrom`;
5. `startReplaceSegments` → upload → `endReplaceSegments`.

> **Consequence: the segment naming scheme is load-bearing.** It is the only index you have for
> *"which existing segments cover this `(merchant, period)`"* — Pinot will not answer that from the
> data. If the name does not encode the partition key, step 4 has no cheap answer and you are reduced
> to rewriting the table.

### Which APIs — and is a thin Java layer needed?

Three controller REST calls bracket the push. Verify exact paths and parameters against the Swagger
on your own controller (`/help`) — these drift between versions:

| Step | Call |
|---|---|
| find what to retire | `GET /segments/{table}?type=OFFLINE` → filter by name convention → `segmentsFrom` |
| open the transaction | `POST /segments/{table}/startReplaceSegments?type=OFFLINE&forceCleanup=true`<br>body `{"segmentsFrom":[…],"segmentsTo":[…]}` → returns `segmentLineageEntryId` |
| upload | push each new segment (metadata push, below); uploaded segments stay **unrouted** until the transaction closes |
| commit | `POST /segments/{table}/endReplaceSegments?type=OFFLINE&segmentLineageEntryId={id}` |
| abort | `POST /segments/{table}/revertReplaceSegments?type=OFFLINE&segmentLineageEntryId={id}` |

**Use metadata push, not tar push.** At this segment size, do not stream tarballs through the
controller. Pinot's Spark runners (`SparkSegmentGenerationJobRunner` +
`SparkSegmentMetadataPushJobRunner`, with `pushJobSpec.pushMode: METADATA`) have Spark write segments
straight to the deep store and send the controller only metadata plus a download URI.

**Do not use `consistentDataPush`.** Pinot ships a consistent-push feature
(`batchIngestionConfig.consistentDataPush: true`) that wraps a push in the lineage protocol
automatically — but its semantics are *whole-table*: `segmentsFrom` becomes every segment currently in
the table. For targeted partial replacement that is catastrophic, deleting every partition absent from
this push. It is built for full-dataset refreshes, which this is not.

So a **thin wrapper is genuinely needed** — but it is orchestration, not Java as such: compute
`segmentsFrom`, bracket the push with start/end, revert on failure. On the order of 100 lines. Put it
where the pipeline is already driven — an **Airflow (Python) operator** is the simplest fit for a
Spark 3.3 + Airflow 2 stack; a Java/Scala helper only earns its place if the swap should live inside
the Spark driver.

> **Worked example:** [`docs/examples/pinot-segment-replace/`](examples/pinot-segment-replace/README.md)
> — the Spark/Scala publish job, the lineage-swap helper and the templated job spec.

**Make it crash-safe.** If the job dies between start and end, the entry is left `IN_PROGRESS`, the
new segments are never routed, and the old data keeps serving — a safe failure. The next run clears it
via `forceCleanup=true`, and the failure path should call `revertReplaceSegments` explicitly rather
than relying on that.

What Pinot *does* decide by itself is only reorganisation, never staleness: minion tasks such as
`MergeRollupTask` (and `UpsertCompactionTask` on realtime upsert tables) compact and merge segments
in the background, but they never determine which of your data is superseded.

```yaml
# jobSpec.yaml — enforces a maximum row count per output segment file
maxNumRecordsPerSegment: '500000'
```

```json
"segmentsConfig": {
  "replication": "1",
  "timeType": "MILLISECONDS",
  "timeColumnName": "accountingPeriod",
  "segmentPushType": "REFRESH",
  "segmentPushFrequency": "MONTHLY",
  "minimizeDataMovement": false
}
```

`segmentPushFrequency` tells the system **how often to expect new data updates**.

Reference: [Pinot architecture and concepts](https://docs.pinot.apache.org/architecture-and-concepts/concepts/architecture)

---

# Recovery

Recovery servers run on a **1 hour** cadence; the tolerated **downtime to a merchant is 12 hrs**.

- On recovery, **VOID (tombstone)** all the older rows for a merchant.
- Lambda-style: pick a **recovery timestamp `T`** and recover all events `<= T`.
- The recovery job handles **upserts + deletes**, stamping rows with `recovery_time = T`
  (checkpointing).
- All events `> T` are already present in the realtime table.

This is **majorly an upstream concern** — an RSL bug. If instead it is a reporting bug, recovery is an
overwrite of the reporting output.

## Known flaw — recovery granularity is period-shaped, not merchant-shaped

The design claims **recoverability + schema evolution** as a requirement, but only actually delivers
it for one of the two recovery shapes.

`timeColumnName` is the **accounting period**. Everything Pinot gives for free — time pruning,
retention, `segmentPushFrequency`, `REFRESH` push semantics — is therefore organised **along the
period axis**. Merchant is only a partition-key component; it is not a first-class handle in Pinot.
So the two recovery shapes are not symmetric:

| Recovery shape | Blast radius | Cheap? |
|---|---|---|
| **Open accounting period** (normal workload) | 1 period × all merchants writing into it — the entire segment set for that period is replaced | ✅ Yes — this is what the design was sized for |
| **Single merchant** (RSL bug, 12 hr downtime) | every `(merchant, period)` segment that merchant has **ever** written | ❌ No — unbounded in the merchant's history |

A merchant-scoped recovery has to VOID and rewrite the merchant's rows across **all** of their
periods. There is no time predicate that narrows it, because the corruption is keyed on merchant, not
on period. The `REFRESH` push replaces whole segments, and a segment is shared by nothing else — but
the *number* of segments to rebuild scales with how much history that merchant has.

### The mutable-periods case is where the cost model breaks

The offline-table choice was justified by *"only a small subset of (merchant, month) — the open
period — has to be overwritten."* That assumption is doing all the work. If a merchant has **every
accounting period open and mutable**, then:

- the "small subset" is the merchant's **whole table**,
- every incremental Spark run is a candidate full rewrite for that merchant,
- and `segmentPushFrequency: MONTHLY` is a lie about the real update rate.

Two defences, and only the first is structural:

1. **Periods must actually close.** This is already the invariant in
   [Layer 2](#why-sqs-and-not-flink) — a mutation landing on a closed period does **not** reopen it,
   it emits **reversals and corrections into the open period**. So "all periods mutable" is a
   business-rule failure, not a workload to design for. Cap it explicitly: a merchant may have at
   most **K open periods** (current + prior until books close), and anything older is closed by
   construction. Without that cap the offline table is the wrong choice and the earlier
   *streaming materializer + Iceberg + Pinot* row of the comparison table wins instead.
2. **Make the rewrite cheap when it does happen.** Recovery is driven from **Iceberg**, never from
   Pinot — Iceberg is the source of truth, Pinot is a rebuildable projection. Recovery re-runs the
   `MERGE INTO` for the affected `(merchant, period)` pairs and re-pushes only those segments.

### What that needs, concretely

- **Iceberg partitioning must support a merchant-only predicate.** If the table is partitioned on
  `accounting_period` alone, a single-merchant recovery **full-scans every partition** — the same
  flaw one layer down. Partition on `(accounting_period, bucket(N, merchant_id))`, or at minimum set
  a sort order on `merchant_id` so file-level column stats can prune.
- **Pinot needs merchant pruning too.** Set `segmentPartitionConfig` on `merchant_id` (murmur) so
  the broker prunes merchant-scoped queries and the recovery push touches a known segment set,
  plus a bloom index on `merchant_id`.
- **Segment splitting is what makes the whale case survivable.** `maxNumRecordsPerSegment` means a
  `(merchant, period)` pair is already many segments, and the **M-to-N lineage swap** replaces them
  atomically. A whale-merchant recovery is a *wide* job, not a job blocked on one enormous segment —
  which is why it parallelises in Spark, and why the **12 hr merchant downtime** tolerance is set
  where it is: a full whale rebuild is a multi-hour batch operation, not a minutes-long one.

### Schema evolution has the same asymmetry

- **Iceberg** evolves metadata-only (column IDs) — adding, renaming or reordering a column rewrites
  nothing. This half of the requirement is genuinely met.
- **Pinot offline segments carry the schema they were built with.** Adding a column with a default
  value is fine — old segments serve the default. But a **type change, or backfilling real values
  into an added column, requires rebuilding every segment**, including the frozen ones for closed
  periods. That is the merchant-recovery problem again, at full-table scale.

So the honest bound on both is the same: **cost of a full Pinot re-push from Iceberg**. Every
recovery and every breaking schema change is ≤ that number, and it should be measured and kept as a
known operational figure rather than assumed away.

> **Two numbers to pin down.** `maxNumRecordsPerSegment: 500000` and *"largest segment ≈ 15 GB"*
> imply ~30 KB per row, which is implausible for a denormalized report row — the 15 GB figure is
> probably the whole `(merchant, month)` partition **before** record-count splitting, not one
> segment. Likewise the flow section says `timeColumnName: accountingperiod` while the
> `segmentsConfig` block says `timestampInEpoch`. Both need reconciling before the recovery cost
> above can be turned into a real estimate.

## Solve — decouple the refresh *contract* from the physical segment layout

> *Can the refresh stay period-scoped, but partition by merchant inside the segment?*

Yes — with one correction. Pinot cannot rewrite **part** of a segment, so "partition by merchant
within a segment" has to become two separate things:

- **across** segments → hash-bucket on `merchant_id` (this is what bounds recovery), and
- **inside** a segment → `sortedColumn: merchant_id` (this is what makes it surgical and fast).

The enabling mechanic is that **Pinot's replacement unit is an arbitrary segment list you name**, not
a fixed grain tied to `timeColumnName`. `startReplaceSegments` / `endReplaceSegments` take an explicit
*M* old → *N* new set. So the *logical* contract can stay "period `P` is refreshed" while the
*physical* swap is narrowed to only the `(period, bucket)` pairs the hourly Spark run actually
dirtied. `timeColumnName` keeps doing time pruning and retention; it stops dictating blast radius.

### The reading that makes it worse

One segment per accounting period, with merchant only as an internal dimension:

- a single-merchant fix now rewrites **every merchant's** data for that period, and
- a period segment holding all 53 k merchants is enormous.

Strictly worse than today's `(merchant, month)`. Discard this one.

### The reason the instinct is right anyway — segment-count explosion

Today's `(merchant, month)` partitioning is *already* minimal for merchant-recovery blast radius —
you touch that merchant's rows and nobody else's. But it has a worse operational flaw than the
recovery one:

| | `(merchant, month)` | `(month, bucket(32, merchant))` |
|---|---|---|
| Segments (53 k merchants × 24 periods) | **~1.27 M** | 24 × 32 = 768, record-split → **~34 k** |
| Median merchant per segment | 1 900 rows ÷ 24 ≈ **79 rows** | n/a — packed with bucket-mates |
| Avg segment size | tiny | **500 k rows** (`maxNumRecordsPerSegment`) |
| Health | ❌ ZK/Helix metadata pressure, million-way broker fan-out | ✅ normal |

A million near-empty segments is a Pinot anti-pattern well before recovery is ever exercised.

### Recommended — tier the layout by merchant size

Skew is the deciding factor: OpenAI is **15%** of all rows, so a plain hash bucket containing it
carries ~**6×** its neighbours. Split the table's layout in two:

| Tier | Layout | Recovery unit | Why |
|---|---|---|---|
| **Whales** (top ~100 merchants) | dedicated `(merchant, period)`, record-count split | that merchant's segments only — ~5 100 for OpenAI at 2.55 B rows | few whales ⇒ segment count stays small; recovery touches **no other merchant** |
| **Long tail** (~53 k) | `(period, bucket(N, merchant_id))`, `sortedColumn: merchant_id` | one `(period, bucket)` ≈ **22 M rows** | bounded segment count and healthy segment sizes; 22 M rows is minutes in Spark even to fix an 79-row merchant |

Recovery cost then scales with the **merchant's own size**, which is the property the previous
section said was missing.

<sub>Row counts derived from *OpenAI = 2.55 B = 15% of rows* ⇒ ~17 B total; ÷ 24 periods ÷ 32 buckets
≈ 22 M per `(period, bucket)`. Cross-check: JE 474 RPS ≈ 41 M/day ≈ 30 B over 2 yrs — same order.</sub>

### Footguns

- **The Murmur function must match on both sides.** `segmentPartitionConfig` pruning is only correct
  if Spark physically wrote each segment with a single partition ID. Pinot's `Murmur` is **Murmur2**
  with its own seed; Spark's `hash()` is **Murmur3** — they do **not** agree. Verify against the
  `PartitionFunctionFactory` of the Pinot version in use, and prefer computing the partition ID as an
  explicit column via a UDF replicating Pinot's function, then `repartition` on that column.
- **Mixed partitions inside a segment silently disable pruning** — it degrades to a full scan rather
  than erroring, so assert purity in the build job.
- **Enable the pruner.** `segmentPartitionConfig` alone does nothing; the table's `routing` block
  needs the partition segment pruner turned on.
- **`sortedColumn` is one column per segment.** Spend it on `merchant_id` — period is already pruned
  at segment level by `timeColumnName`, and sorting gives run-length compression on the merchant
  column plus contiguous merchant rows, so a recovery touches few segments per period.

### What this does *not* fix

It bounds the cost **per period touched**, and it fixes segment-count explosion. It does **not**
bound the **number of periods** a merchant recovery has to walk — nothing in the physical layout can.
Only the *books actually close* invariant (at most **K** open periods) bounds that, which is why that
remains the load-bearing assumption of the whole serving design.
