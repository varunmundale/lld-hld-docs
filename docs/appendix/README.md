# Appendix — Index

Reference material behind the problems: the consistency models and storage/streaming
technologies the HLD designs lean on, and the concurrency primitives the LLD designs lean on.

## [HLD Appendix](hld.md)

*From `hld_appendix.xopp`, 24 pages.*

**[A) Different Types of Consistencies](hld.md#a-different-types-of-consistencies)** — worked
through seven topologies, each with what it does and does not guarantee:

1. [Single node, single thread](hld.md#1-single-node-with-single-thread)
2. [Single leader + replica (asynchronous)](hld.md#2-single-leader--replica-asynchronous)
3. [Single node + replica (synchronous)](hld.md#3-single-node--replica-synchronous)
4. [Single node, multiple threads](hld.md#4-single-node-with-multiple-thread)
5. [Partitioning (sharding)](hld.md#5-partitioning-sharding)
6. [Sharding + replication, leaderless](hld.md#6-sharding--replication-leaderless) — Cassandra-style, tunable consistency
7. [Sharding + replication, leader-based](hld.md#7-sharding--replication-leader-based) — Spanner/CockroachDB-style
   · [Linearizability](hld.md#linearizability-guarantee) · [Serializability](hld.md#serializability-guarantee)

**[B) Technologies](hld.md#b-technologies)**

| | Technology | Covers |
|---|---|---|
| 1 | [Elasticsearch](hld.md#1-elastic-search-eventual-consistent) | [ingestion](hld.md#ingestion), [search](hld.md#search), [when to use](hld.md#when-to-use), [nodes](hld.md#nodes), [indexing & search sequence diagram](hld.md#elasticsearch-indexing--search-sequence-diagram) |
| 2 | [Cassandra](hld.md#2-cassandra) | [query-driven data model](hld.md#data-model-aka-query-driven) |
| 3 | [Kafka](hld.md#3-kafka) | [when to use Kafka](hld.md#when-to-use-kafka) |
| 4 | [Redis](hld.md#4-redis) | [sorted sets](hld.md#sorted-sets), [geospatial index](hld.md#geospatial-index), [pub/sub](hld.md#pubsub) |
| 5 | [Iceberg](hld.md#5-iceberg) | table format, upserts |
| 6 | [Pinot](hld.md#6-pinot) | [architecture](hld.md#pinot-architecture) |

## [LLD Appendix](lld.md)

*From `lld_appendix.xopp`, 12 pages.*

**[A) Concurrency](lld.md#a-concurrency)** — the [three problem types](lld.md#3-problem-types)
and the [reference table](lld.md#reference-table-from-embedded-image):

1. [Correctness](lld.md#1-correctness) — [coarse-grained locking](lld.md#coarse-grained-locking),
   [read-write locks](lld.md#read-write-locks), [fine-grained locks](lld.md#fine-grained-lock),
   [atomic variables](lld.md#atomic-variables), [common bugs](lld.md#common-bugs), [problems](lld.md#problems)
2. [Coordination](lld.md#2-coordination) — [solution](lld.md#solution),
   [blocking queue](lld.md#blocking-queue), [message passing / actor model](lld.md#message-passing-coordination)
3. [Scarcity](lld.md#3-scarcity)

**[B) Task Execution Engine](lld.md#b-task-execution-engine)** — DAG-based orchestration:
[requirements](lld.md#requirements) and the naive per-task lock/wait approach, then the
[better approach](lld.md#better-approach) using a `remainingDeps` counter.

## Cheat sheets

*From the `HLD quick cheat sheet` spreadsheet — one file per tab.*

- **[Isolation levels](isolation-levels.md)** — [the four levels](isolation-levels.md#a-the-four-levels),
  [anomaly matrix](isolation-levels.md#b-anomaly-matrix), [the four anomalies](isolation-levels.md#c-the-four-anomalies),
  [picking a level by use case](isolation-levels.md#d-picking-a-level-by-use-case),
  [decision tree](isolation-levels.md#e-decision-tree)
- **[Database decision list](database-decision-list.md)** — workloads that push teams onto
  distributed SQL, plus the [best-suited database](database-decision-list.md#best-suited-database)
  per workload, [use cases the sheet misses](database-decision-list.md#use-cases-the-sheet-misses),
  a [database map of every HLD problem](database-decision-list.md#problem-set-database-map),
  [the candidates](database-decision-list.md#the-candidates) compared, and
  [when you don't need distributed SQL](database-decision-list.md#when-you-dont-need-distributed-sql)

## Related

- [HLD problems](../hld/README.md)
- [LLD problems](../lld/README.md)
