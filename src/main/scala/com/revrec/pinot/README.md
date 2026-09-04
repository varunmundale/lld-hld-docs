# Demo — targeted M-to-N segment replacement, Spark/Scala

Companion to [Revenue Recognition — End to End](../../../../../docs/revenue_recognition_pipeline.md#which-apis--is-a-thin-java-layer-needed).
Replaces the Pinot segments backing **one `(accounting_period, merchant_bucket)` partition** — the
unit the hourly run dirties — leaving every other partition untouched.

> **Illustrative, not compiled.** These files show the shape of the production job; they are not
> wired into the Maven build (they reference Spark, Iceberg and Pinot classes this repo does not
> depend on, so adding them to `src/` would break `mvn compile`). Pinot API signatures and job-spec
> config keys drift between versions — check them against your controller's Swagger (`/help`).

| File | Role |
|---|---|
| `PublishReportToPinot.scala` | The Spark job: finds dirty partitions, reads each back in full from Iceberg, writes parquet for segment generation. |
| `PinotSegmentSwap.scala` | Brackets generation + push in the segment lineage transaction. |
| `JobConfig.scala` | Loads `publish.yaml`. |
| `jobspec.yaml` | Pinot's own ingestion spec — templated, driven twice (`SegmentCreation`, then `SegmentMetadataPush`). |
| `publish.yaml` | This job's config: source, layout, controller, and swap behaviour. |

### Where configuration lives

Two YAML files, split by who reads them — nothing operational should need a recompile:

- **`jobspec.yaml`** is Pinot's own spec, consumed by the ingestion runners. `pushJobSpec`
  (`pushParallelism`, `pushAttempts`, `pushRetryIntervalMillis`, `copyToDeepStoreForMetadataPush`)
  belongs here because `SparkSegmentMetadataPushJobRunner` reads it directly.
- **`publish.yaml`** is what *our* code acts on: the Iceberg source and snapshot watermark, the
  layout (`numBuckets`, `filesPerPartition`, staging base), the controller/table coordinates, and the
  swap behaviour (`forceCleanup`, `revertOnFailure`, `transactionScope`, and the two refusal guards).

Only `--run-id` and the optional `--snapshot-id` come in as arguments, because they are per-run
rather than per-environment:

```bash
spark-submit --class com.revrec.pinot.PublishReportToPinot app.jar \
  --config publish.yaml --run-id 20260827T120000
```

Two settings there are load-bearing rather than tuning knobs. `layout.numBuckets` must equal
`segmentPartitionConfig.numPartitions` in the Pinot table config — changing it invalidates every
existing segment name and forces a full rebuild. And `swap.transactionScope` must never widen to the
whole run: `segmentsFrom`/`segmentsTo` live in one ZooKeeper znode, and a whale merchant's history
would put ~10k segment names into a single entry against ZK's 1 MB default `jute.maxbuffer`.

`SparkSegmentGenerationJobRunner` and `SparkSegmentMetadataPushJobRunner` are **not classes you
write** — they are Pinot classes you *name* in a spec and invoke. The deliverable is the Spark job
that produces their input and the orchestration that makes the swap atomic.

## Flow

```
  MERGE INTO iceberg (hourly, upstream)
        │
        ▼
  dirtyPartitions()          ← the delta's ONLY job: name which partitions changed
        │
        ▼   for each (period, bucket)
  writeFullPartition()       ← COMPLETE partition re-read from Iceberg, not the delta
        │                      coalesce → partition-pure files
        │                      sortWithinPartitions(merchant_id) → matches sortedColumn
        ▼
  SparkSegmentGenerationJobRunner        → segment tarballs in deep store
        │
        ▼
  GET  /segments        → filter by prefix → segmentsFrom
  POST /startReplaceSegments             → entryId   (new segments not yet routed)
  SparkSegmentMetadataPushJobRunner      → metadata + download URI only
  POST /endReplaceSegments               → atomic swap
```

## Why it is one Spark application

`SparkSegmentGenerationJobRunner` parallelises over its input files on the *ambient* SparkContext, so
generation runs inside the same app that wrote the parquet — one cluster allocation, one set of
credentials, one failure domain. Shelling out to a second `spark-submit` per partition would pay
allocation cost per partition and split the failure handling in two.

## Why one lineage transaction per partition

A failure isolates to a single partition, and the ZooKeeper znode holding `segmentsFrom` /
`segmentsTo` stays small. A whale merchant's full-history recovery would otherwise put ~10k segment
names into one entry, against ZK's 1 MB default `jute.maxbuffer`. The trade is global atomicity for
per-period atomicity — the right boundary for an accounting ledger, since the period is already the
unit that closes.

## The three things this exists to get right

**1. Push the full partition, never a delta.** A Pinot offline table has no primary key and no
upsert; it appends what it is given. A delta pushed under a new segment name double-counts silently
in an accounting ledger.

**2. Segment names must be unique per run.** `segmentsTo` may not name segments that already exist,
so a deterministic name collides with the very segments being replaced. The run id sits to the right
of the partition prefix, so names stay unique while the prefix remains the index for computing
`segmentsFrom`.

**3. Pinot guarantees atomicity, not conservation.** It never checks that `segmentsTo` covers the
rows in `segmentsFrom` — a short list is an atomic, successful, silent delete. Hence the two
`require` guards before the transaction opens.

## Two footguns the code defends against

- **Murmur2 vs Murmur3.** Pinot's `Murmur` partition function is Murmur2 with its own seed; Spark's
  `hash()` is Murmur3. Bucketing with the wrong one produces segments that are not partition-pure,
  which **disables broker pruning silently rather than erroring**. The job calls Pinot's own
  `MurmurPartitionFunction` instead of reimplementing it.
- **`consistentDataPush`.** Pinot's built-in consistent push wraps a push in this same protocol
  automatically — but with whole-table semantics (`segmentsFrom` = every segment in the table). It is
  built for full-dataset refreshes; here it would delete every partition absent from the push.
