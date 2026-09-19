# Lead Data Engineer — quick notes

[← Data engineering index](README.md) · [All docs](../README.md)

---

*(condensed from a 2026-09-18 prep session for a lead DE loop — round 1 is
"discussion + fundamentals". Quiz cards scored 5/5 SQL semantics, 3/5 design
fundamentals R1, 4/5 design fundamentals R2.)*

## 1. Topic checklist for the fundamentals round

The round tests depth of reasoning, not trivia — for every item be ready to say **why** it
behaves that way.

| Area | Must be able to explain |
|---|---|
| **SQL** | window functions (rank, lag/lead, running totals), CTEs + recursive CTEs, join semantics and when a join multiplies rows, dedup, gaps-and-islands, NULLs in aggregates; execution plans, index vs scan, predicate pushdown, why GROUP BY on a skewed key hurts |
| **Modeling** | OLTP vs OLAP, (de)normalization tradeoff, star vs snowflake, **fact grain**, dimension types, SCD 1/2/3 with real merge logic, surrogate keys, late-arriving dims and facts, Data Vault conceptually |
| **Spark** | driver/executor, jobs → stages → tasks, lazy eval, narrow vs wide, what a shuffle costs; partitioning vs bucketing, broadcast join threshold, skew (salting, AQE), repartition vs coalesce, cache/persist backfiring, OOM causes, Catalyst + AQE |
| **Storage** | row vs columnar, Parquet internals (row groups, column chunks, footer stats, pushdown), Parquet vs ORC vs Avro (when Avro wins), small-files problem; Delta / Iceberg / Hudi — ACID on object store, time travel, schema evolution, compaction, metadata design differences |
| **Streaming** | Kafka partitions, consumer groups, offsets, rebalance, replication/ISR, idempotent producer, what exactly-once actually covers; event vs processing time, watermarks, tumbling/sliding/session windows, late data, checkpointing; Debezium CDC |
| **Orchestration** | idempotent re-runnable tasks, incremental loads + watermark bookkeeping, backfills, retries and poison messages, dependencies, freshness SLAs; Airflow scheduler/sensors/dynamic DAGs/anti-patterns; dbt |
| **Warehouse internals** | Snowflake micro-partitions, clustering keys, warehouse sizing/credits; BigQuery slots, partition + cluster; Redshift dist/sort keys; cost optimisation |
| **Architecture** | Lambda vs Kappa, medallion, batch-vs-stream criteria, lakehouse vs warehouse, CAP/consistency, schema contracts producer↔consumer, DQ frameworks and where checks live, lineage/governance, PII + GDPR deletes in an append-only lake |

**Short-on-time priority (six items cover most of it):** SQL windows + optimisation · fact
grain + SCD2 · Spark shuffle + skew · Parquet + one table format · Kafka semantics +
watermarks · idempotent incremental loads.

## 2. The discussion half — have ready

- One architecture you can sketch end to end in two minutes **with real numbers**: volume/day,
  row counts, latency, cost, team size.
- Three or four A-over-B decisions, including what you gave up.
- One incident: what broke, how you found it, what changed structurally after.
- One migration / cost-reduction story with a before/after number.
- Lead-flavoured: code review, standards, onboarding, design disagreement with a senior,
  pushing back on a stakeholder.
- "What would you do differently now?" about the system you whiteboard — a vague answer reads
  as not having owned it.

## 3. SQL patterns

### 3.1 Sessionization — gaps-and-islands (the pattern to have in your fingers)

`events(user_id, event_time, event_type)`, a session breaks on > 30 min inactivity. Three
steps:

1. **Look back** — `LAG(event_time) OVER (PARTITION BY user_id ORDER BY event_time)`.
2. **Boundary flag** — `1` if gap > 30 min **or** prev is NULL (first event), else `0`.
3. **Running SUM over the flag** → group label. Every row carries the count of boundaries at
   or before it, so all rows of one session share one integer.

```sql
WITH lagged AS (
  SELECT user_id, event_time, event_type,
         LAG(event_time) OVER (PARTITION BY user_id ORDER BY event_time) AS prev_event_time
  FROM events
),
flagged AS (
  SELECT *,
         CASE WHEN prev_event_time IS NULL THEN 1                              -- first event: put this branch FIRST
              WHEN event_time - prev_event_time > INTERVAL '30 minutes' THEN 1 -- strictly >, not >=
              ELSE 0 END AS is_session_start
  FROM lagged
),
numbered AS (
  SELECT *,
         SUM(is_session_start) OVER (PARTITION BY user_id ORDER BY event_time
                                     ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS session_idx
  FROM flagged
),
identified AS (
  SELECT *,
         MD5(CAST(user_id AS VARCHAR) || '|' ||
             CAST(MIN(event_time) OVER (PARTITION BY user_id, session_idx) AS VARCHAR)) AS session_id
  FROM numbered
)
SELECT session_id, user_id,
       MIN(event_time) AS session_start, MAX(event_time) AS session_end, COUNT(*) AS event_count
FROM identified
GROUP BY session_id, user_id;
```

Things graded on / mistakes I made in the first attempt:

- **Flag ≠ group.** Going straight from the boundary flag to a `session_id` gives an id only
  to the boundary row and `0` to the rest — you can't group. The running SUM is the missing
  step; it's the canonical idiom, not a hack (SQL has no "group consecutive rows until a
  condition breaks" operator; alternatives are O(n²) self-joins, correlated subqueries, or a
  recursive CTE that won't parallelise).
- `prev - event_time` is backwards (always negative, never fires). Use `event_time - prev`.
- `> 30 min`, not `>=` — exactly 30 min of silence is still the same session.
- NULL lag → comparison is UNKNOWN → falls to `ELSE 0` → first event never starts a session.
  Explicit `IS NULL` branch first.
- `timestamp(30, mins)` isn't syntax: `INTERVAL '30 minutes'` (Postgres) or
  `unix_timestamp` diff vs `1800` (Spark).
- CASE branches must return the same type. Every CTE needs a `FROM`. The question asked for a
  final aggregate — don't stop at the labelled events.
- **Explicit `ROWS BETWEEN … CURRENT ROW`** — the default `RANGE` frame lumps together rows
  with identical `ORDER BY` values, and duplicate timestamps are common in clickstream.

**Why not `uuid()` for `session_id`.** It's non-deterministic: every re-run / backfill mints
new ids, downstream joins break, tests can't assert on output, two derivations never line up.
`MD5(user_id || '|' || session_start)` is unique by construction and reproducible forever.
Interview line: *"running-sum-over-boundary-flag, the standard gaps-and-islands approach — one
pass, no self-join. For the id I hash user_id + session_start so the pipeline stays idempotent
under backfills."*

If a random uuid is required anyway: generate it **at the session grain** (a `GROUP BY user_id,
session_idx` CTE with `gen_random_uuid()` / Spark `uuid()`), then join back to events. Don't
shortcut with `FIRST_VALUE(uuid()) OVER (...)` — per-row vs folded evaluation isn't guaranteed
across engines. In Spark, `uuid()` is marked non-deterministic so a **task retry can regenerate
different values within the same run** — write the session-key table out (or `persist()` +
action) before consuming it. Production version: mint once when a session is first seen,
store in a session dimension, look it up on later runs.

Follow-ups a lead interviewer pivots to:

- session_idx is only unique per user — real id? (hash, above)
- **Incremental**: a session can straddle midnight, so yesterday's last session may need
  reopening. Either look back one partition and recompute, or keep an open-sessions state table.
- Spark on a billion events with a few bot users holding millions each → the single
  user partition is the bottleneck (LAG forces shuffle-on-user + sort).
- On a stream: Flink / Structured Streaming `session_window` is the native answer.

### 3.2 LAG

Reach back N rows within the partition and fetch a column: `LAG(col, n=1, default=NULL) OVER
(PARTITION BY … ORDER BY …)`. `LEAD` is the mirror. It **does not skip NULLs** and there's no
filtering inside it — it's the previous row in the window, full stop. Uses: deltas between
readings, state-change detection (tier differs from previous snapshot → SCD2 from snapshots),
time between events, first step of nearly every gaps-and-islands problem. Cost: forces sort
within partition → in Spark a shuffle on the partition key + sort.

### 3.3 Longest streak (Q2 — still open, solution not yet posted)

`user_activity(user_id, activity_date)`, one row per active day. Return the longest run of
consecutive days per user + its start date.

- Option A: LAG + running-sum, exactly as 3.1 with `activity_date - prev = 1 day` as the
  "same island" condition.
- Option B (the famous one): `activity_date - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY
  activity_date)` is **constant within a run** of consecutive days → use it directly as the
  group key, no flag, no cumulative sum. Then `GROUP BY user_id, grp` → `COUNT(*)`, `MIN(date)`;
  pick the max per user.
- Sample: user 1 = {03-01, 03-02, 03-03, 03-07, 03-08} → streak 3 from 03-01. User 2 =
  {03-05, 03-09} → streak 1; state the tie-break (earliest start) explicitly.

### 3.4 SCD2 from daily snapshots (Q3 — not yet attempted)

`daily_snapshot(snapshot_date, customer_id, tier, region, email)`, two years of full dumps.
Collapse to one row per customer per distinct attribute combination with `effective_from`,
`effective_to`, `is_current`. Gold → Silver → Gold must give **three** rows (so hash-of-
attributes ≠ LAG-of-attributes: compare to the previous *day*, not to any prior value). Missing
snapshot days (outages) — say what you'd do: carry forward / treat as no change, and flag it.

### 3.5 Top-10 per country per day at 50 B rows (Q4 — reasoning, not yet attempted)

Naive `ROW_NUMBER() OVER (PARTITION BY country, day ORDER BY clicks DESC)` on the raw table
shuffles everything and the 60 %-country partition becomes one straggler. Aggregate first
(`GROUP BY country, day, item` with partial aggregation), then rank on the much smaller
result; salt the hot country; AQE skew join; prune to 90 days via the partition column.

### 3.6 RAND, seeds, salting, sampling

- There's no `map(fn, col)` in SQL — the SELECT list *is* the map. `MAP<K,V>` is a **type**
  (Spark/Hive/Presto/BQ) with `map_keys`, `map_values`, `transform_values`; Spark's
  `transform(arr, x -> …)` maps over an array *inside* one row.
- `RAND()` / `RANDOM()` → float in [0,1), evaluated per row, non-deterministic.
  `RAND(42)` — **the arg is a seed, not a range**; still returns [0,1). Random int in a range
  is arithmetic: `FLOOR(RAND() * (hi - lo + 1)) + lo`.
- Spark combines the seed with the partition index → reproducible only if partitioning is
  identical. For a genuinely stable sample, hash the key: `WHERE ABS(HASH(user_id)) % 100 = 0`.
  That's the answer to "how do you get a stable 1 % sample".
- Engine table: Spark `RAND(42)` / `RANDN(42)`; Postgres `SETSEED(0.42)` then `RANDOM()`;
  MySQL `RAND(42)`; **Snowflake `RANDOM()` returns a 64-bit int, so `RAND() < 0.01` silently
  breaks — use `SAMPLE`**; BigQuery has no seed — `FARM_FINGERPRINT(key)`.
- `TABLESAMPLE (1 PERCENT) REPEATABLE (42)` skips whole blocks — cheaper than a RAND filter.
- **Salting a skewed join**: fat side `CONCAT(key, '_', FLOOR(RAND() * 50))`; thin side
  `CROSS JOIN explode(sequence(0, 49))`; join on the salted key; aggregate the salt away.
- Same retry caveat as `uuid()`: an unmaterialised RAND sample can change under task retry /
  lineage recomputation.

### 3.7 Quiz gotchas worth remembering

- `NOT IN (subquery)` where the subquery contains a NULL → **0 rows** (every comparison is
  UNKNOWN). Use `NOT EXISTS`.
- Default window frame with `ORDER BY` is `RANGE … CURRENT ROW` → ties collapse. Be explicit.
- Grain = the precise definition of what one row in the fact represents. Say it unprompted
  ("one row per order line item per status change").
- Kafka exactly-once (transactional producer + `read_committed`) covers **Kafka-internal**
  reads/writes only. A consumer writing Parquet to S3 is *not* exactly-once end to end — the
  external sink needs its own idempotency (idempotent writes / table-format commit).
- Items flagged to revisit after R1: grain, idempotency, partition cardinality, CDC, SCD2.
  R2 topics worth a longer treatment: the exactly-once boundary, Delta/Iceberg commit mechanism.

## 4. "LLD" in a data-engineering loop

Not class diagrams (unless it's a platform team — then expect it). Three shapes:

1. **Pipeline design** (most common) — clickstream Kafka → lakehouse hourly; CDC Postgres →
   warehouse; 500 files/day from 40 vendors with inconsistent schemas; 5-min real-time
   dashboard. Graded on: ingestion, storage layout + partitioning, formats + compaction,
   batch/stream call, idempotency + replay, schema evolution, failure handling + retries, DQ
   gates, orchestration, monitoring/SLAs, cost.
2. **Data modeling** — returns process, ride lifecycle, subscription billing, multi-tenant
   SaaS. Wanted: explicit fact grain, dimensions, SCD choice per dim, bridge tables for
   many-to-many, degenerate dims, **accumulating snapshot facts** for lifecycles, additive vs
   semi-additive measures, late-arriving data.
3. **Component / framework design** (closest to real LLD) — config-driven ingestion framework
   for 50 sources, DQ framework, metadata-driven pipeline generator, backfill service, schema
   registry client. ABCs for source/sink connectors, strategy for validation rules, factory for
   readers, plugin architecture. Expect this if the JD says "platform" or "framework".

**What separates a lead answer** (everyone draws the same boxes):

- Ask about scale before drawing — volume, velocity, freshness SLA, consumers, budget.
  Three minutes of questions beats thirty seconds to the first box.
- Grain discipline, unprompted.
- Idempotency + replay everywhere — "rerun day 47 after a bug fix and get identical output".
- Failure modes — late source, silent schema change, restating last quarter's metric.
- Tradeoffs **with a decision**: "batch, because a 15-min SLA doesn't justify streaming ops;
  at 1 min I'd switch to Flink". Name what you give up.
- Cost awareness — leads own budget.
- Operability — alerting, on-call, runbooks, lineage, debugging at 3 am.

**Pipeline-question flow:** requirements + scale → sources + ingestion → storage layout →
processing → serving → quality + observability → orchestration + failure handling → cost +
scaling → tradeoffs you'd revisit.

## 5. Open threads

- Mock LLD round (default prompt: clickstream → lakehouse) — highest value, not yet done.
- Spark question still open: job crept 25 min → 2 h then OOM'd — diagnosis, root causes,
  fixes, broadcast joins, **null-key skew**.
- Behavioral / leadership half — untouched; often half the loop at lead level.
- Q2 / Q3 / Q4 above — write them out.
