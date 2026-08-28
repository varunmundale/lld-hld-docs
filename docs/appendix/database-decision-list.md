# Database Decision List

*The table immediately below is transcribed from the
[HLD quick cheat sheet](https://docs.google.com/spreadsheets/d/1OOnBR6kKoLRFBjePAI9D-Q152yAWHgqYOaWh_ma_KfY/edit?gid=768645408#gid=768645408)
spreadsheet, `Database decision-list` tab. Everything after it is added, not from the sheet:*

- [Best-suited database](#best-suited-database) — which store actually fits each of the sheet's six rows, and why
- [Use cases the sheet misses](#use-cases-the-sheet-misses) — nine more patterns from the problem set
- [Problem-set database map](#problem-set-database-map) — every HLD problem, its primary store, and the reasoning
- [The candidates](#the-candidates) — fifteen stores compared
- [Key design per store](#key-design-per-store) — partition, clustering and primary keys, store by store
- [When you don't need distributed SQL](#when-you-dont-need-distributed-sql)

Workloads that push teams onto distributed SQL (Spanner / CockroachDB / TiDB / YugabyteDB
class), with the requirement that forced the move and who runs it in production. Useful as
a "name a real company" answer when justifying a database choice in an interview.

| Use case | Core database requirement | Real-world examples | Why they use it |
|---|---|---|---|
| **Financial ledgers & digital wallets** | Strict ACID compliance to prevent double-spending; multi-region active-active setup for zero downtime. | FanDuel, Groww, PayPal, JPMorgan Chase | Processing multi-billion dollar transactions across regions. Traditional sharding risks data corruption/skew, whereas distributed SQL guarantees instant global consistency. |
| **Global order management & inventory** | Real-time global inventory accuracy; handling massive, unpredictable traffic spikes without locking. | Booking.com, Walmart, DoorDash | Eliminates overselling/double-booking. During flash sales or peak hours it scales out horizontally across cloud regions while maintaining a single source of truth. |
| **Identity & access management (IAM)** | Ultra-low read latency globally, high availability, and localized data compliance. | Netflix, Squarespace | Powers authorization and session state for millions of concurrent users. A user logging in anywhere globally gets instant authentication without cross-ocean network lag. |
| **Core payment platforms** | High-throughput OLTP with automated multi-site failover. | SumUp, Block (Square) | Merchant payments must process instantly and securely. If a cloud availability zone drops completely, the database handles failover automatically with zero data loss (`RPO = 0`). |
| **Online gaming backends & betting** | Massive write-heavy workloads; strict compliance with data residency laws. | Devsisters, Hard Rock Digital, Kaizen Gaming | Player balances, game states and live sports bets must be updated perfectly in real time. Geo-partitioning pins user data to specific physical regions to comply with localized gaming regulations. |
| **Global SaaS control planes** | Multi-tenant isolation, effortless cross-region replication, and zero-downtime schema migrations. | Cisco AI, Starburst, Mux | They migrated away from legacy databases (e.g. PostgreSQL shards) because managing multi-region manual sharding became an operational nightmare. Distributed databases let them treat the cluster as one giant global machine. |

---

## Best-suited database

> **Read the source list critically.** Nearly every company above (FanDuel, DoorDash,
> Netflix, Squarespace, SumUp, Devsisters, Hard Rock Digital, Starburst, Mux) appears in
> CockroachDB's published case studies, so the list is a vendor roster rather than an
> independent survey. The honest answer to "which database" is CockroachDB or Spanner for
> most rows — what actually differs is **which part of the workload belongs in it**, and
> that's where the marks are in an interview.

| Use case | Primary pick | Credible alternatives | Don't make this the system of record |
|---|---|---|---|
| Financial ledgers & wallets | **Spanner** (all-in on GCP) or **CockroachDB** (multi-cloud / on-prem) | TiDB, YugabyteDB; Oracle/DB2 if it's already there | Cassandra, DynamoDB, MongoDB |
| Global order management & inventory | **CockroachDB** | YugabyteDB, TiDB; Vitess/MySQL if cleanly partitionable | Any eventually-consistent store for the stock count |
| Identity & access management | **Split it:** PostgreSQL or CockroachDB for identity + **Redis / DynamoDB Global Tables** for sessions | Spanner for identity; Valkey or Memcached for sessions | A single-region Postgres primary serving global auth writes |
| Core payment platforms | **CockroachDB** or **Spanner** | YugabyteDB, TiDB | Anything replicating asynchronously, if you claim `RPO = 0` |
| Online gaming & betting | **CockroachDB** (`REGIONAL BY ROW`) for balances/bets + **Redis** for live state | YugabyteDB (tablespaces) for residency | Distributed SQL for per-tick game state |
| Global SaaS control planes | **CockroachDB** | PostgreSQL + Citus (single region); Vitess for MySQL shops | Hand-rolled application-level sharding |

### Financial ledgers & digital wallets → Spanner or CockroachDB

Preventing double-spend means one atomic transaction spanning at least two rows (debit
and credit) that may live in different shards and different regions. That is exactly the
guarantee only distributed SQL offers cheaply.

- **Spanner** gives *external consistency* — TrueTime bounds clock uncertainty with GPS
  and atomic clocks, so a transaction that commits before another starts is always
  ordered that way globally. Strongest guarantee available; the trade is GCP lock-in.
- **CockroachDB** defaults to `SERIALIZABLE` and uses hybrid logical clocks instead of
  atomic clocks — slightly weaker ordering guarantees across regions, but it runs on any
  cloud or on-prem, which is why regulated fintechs pick it.

**Avoid** Cassandra or DynamoDB as the ledger's system of record. Cassandra resolves
conflicts by last-write-wins timestamps, which silently discards a concurrent write;
lightweight transactions fix that but cost a Paxos round per operation. DynamoDB's
`TransactWriteItems` is capped at 100 items in a single region — useful, but not a global
serializable transaction. Both are fine as *derived* read models fed from the ledger.

### Global order management & inventory → CockroachDB

The real enemy here is not isolation level, it is the **hot row**: every buyer of the same
SKU contends on one counter, and no database makes a single row scale. Serializable
isolation makes overselling impossible, but under a flash sale it converts overselling
into a retry storm.

Fix it in the schema, not the isolation level — split the counter into N reservation rows
per SKU and pick one at random, or move to a reserve-then-confirm flow where the reservation
is an insert (contention-free) rather than a decrement. Then CockroachDB's horizontal write
scaling actually buys you something.

If orders partition cleanly by seller or warehouse and you never need one atomic
transaction across two partitions, **Vitess on MySQL** is a cheaper answer.

### Identity & access management → split the workload

This row is the one most often got wrong. "IAM" bundles two workloads with opposite needs:

| | Identity data (users, roles, permissions) | Session / token validation |
|---|---|---|
| Write volume | Low | High (every login) |
| Read volume | High | Very high |
| Latency budget | ~10 ms | ~1 ms, on every request |
| Consistency need | Strong — a revoked permission must stick | Weak — tokens are short-lived and expire anyway |
| **Store** | PostgreSQL with read replicas, or CockroachDB if you need multi-region writes | **Redis** (or DynamoDB Global Tables) |

Putting session lookups in a distributed SQL database because identity lives there is a
common and expensive mistake — you pay consensus latency on every single request to
protect data that self-invalidates in fifteen minutes. Netflix's global-low-latency story
is about *reads*, and reads are what a replicated cache does best.

**Avoid** a single-region PostgreSQL primary for global auth: a user in Singapore hitting
a primary in `us-east-1` eats ~200 ms of round-trip on every write.

### Core payment platforms → CockroachDB or Spanner

`RPO = 0` is the whole requirement, and it is a replication property, not a database brand.
Zero data loss requires **synchronous quorum replication** — the write is acknowledged only
after a majority of replicas across failure domains have durably accepted it. Raft
(CockroachDB, TiDB, YugabyteDB) and Paxos (Spanner) do this by construction.

Vanilla PostgreSQL and MySQL replicate asynchronously by default, so a primary that dies
takes its unreplicated tail with it. `synchronous_commit = remote_apply` with a standby
gets you to `RPO = 0`, but a two-node sync pair has no quorum: lose the standby and you
must choose between stalling writes and dropping the guarantee. Failover is also manual or
bolted on (Patroni), which hurts RTO even when RPO is fine.

Worth saying out loud in an interview: exactly-once payment semantics come from
**idempotency keys and the transactional outbox pattern**, not from the database. The
database gives you `RPO = 0`; the application gives you "don't charge the card twice".

### Online gaming & betting → CockroachDB with geo-partitioning, plus Redis

Data residency is the differentiator, and it is a schema feature. CockroachDB's
`REGIONAL BY ROW` tables attach a hidden region column to each row and physically pin its
replicas to that region's nodes — so an Italian player's data provably never leaves Italy,
while the rest of the cluster stays one logical database. That is the compliance argument;
Hard Rock Digital runs it on AWS Outposts for exactly this reason. YugabyteDB reaches the
same place with tablespaces.

Split the workload again: **balances, wagers and settlements** are money and belong in the
transactional database; **live game state, positions and leaderboards** are ephemeral,
update many times a second, and belong in Redis sorted sets. Devsisters' 60k transactions
per second is a number about the former, not the latter.

### Global SaaS control planes → CockroachDB

The pitch here is **online schema changes**. CockroachDB and Spanner run migrations as a
background, versioned, non-blocking process, so `ADD COLUMN` on a large table doesn't take
a maintenance window — for a control plane serving every tenant at once, that alone can
justify the move. Multi-tenant isolation then falls out of the same partitioning machinery
used for residency.

But read the "why" column honestly: these teams fled **manual sharding**, not PostgreSQL.
If you are in one region with a few terabytes, plain PostgreSQL is still the right answer,
and PostgreSQL + Citus covers a lot of the middle ground.

---

## Use cases the sheet misses

The sheet's six rows are all *transactional* workloads, which is why they all land on
distributed SQL. Working through the [HLD problem set](../hld/README.md) surfaces seven
more patterns where the right answer is emphatically **not** a relational database:

| Use case | Core requirement | Primary store | Why | Seen in |
|---|---|---|---|---|
| **Chat & messaging** | Massive append-only writes, ordering *within* a conversation only, TTL'd retention. | **Cassandra** + Redis pub/sub | Partition by `user_id`/`chat_id` and rows arrive pre-sorted by clustering key. No cross-partition ordering is needed, so the one thing Cassandra can't do doesn't matter. | [WhatsApp](../hld/10-whatsapp.md), [FB Live Comments](../hld/19-fb-live-comments.md) |
| **Feeds & timelines** | Precomputed per-user lists, read-heavy, tolerant of seconds of staleness. | **Redis** (sorted sets) + Cassandra | The feed is a materialised view, not truth — rebuildable from posts. Redis gives O(log n) range reads; Cassandra holds the durable copy. | [Instagram](../hld/06-instagram.md), [News Aggregator](../hld/20-news-aggregator.md), [FB News Feed](../hld/07-fb-news-feed.md) |
| **Real-time analytics / top-K** | Pre-aggregated windows, sub-second scans over billions of rows, eventual consistency fine. | **Pinot / Druid / ClickHouse**, fed by Kafka + Flink | Columnar storage plus pre-aggregation answers "top K in the last hour" without touching raw events. A row store would scan far too much. | [Ad Click Aggregator](../hld/27-ad-click-aggregator.md), [YouTube Top K](../hld/22-youtube-top-k.md), [Metrics Monitoring](../hld/30-metrics-monitoring.md) |
| **Full-text & geo search** | Ranked relevance over text, faceting, radius queries. | **Elasticsearch** (or PostgreSQL + PostGIS at low scale) | Inverted indexes and BM25 scoring are not something a B-tree does. Keep the source of truth relational and treat the index as derived. | [TicketMaster](../hld/05-ticketmaster.md), [FB Post Search](../hld/28-fb-post-search.md), [Yelp](../hld/03-yelp.md) |
| **Large media / blobs** | Cheap durable bytes, range reads, CDN origin. | **S3** + a relational metadata table | Never put file bytes in a database. Store the bytes in object storage and the pointer plus ACL in Postgres. | [Dropbox](../hld/02-dropbox.md), [YouTube](../hld/17-youtube.md) |
| **Ephemeral hot state** | Sub-millisecond reads/writes, TTL semantics, durability explicitly *not* required. | **Redis** | Driver locations, rate-limit counters, HOLD locks and live game state are all rebuildable or expiring. Paying for durability here is waste. | [Rate Limiter](../hld/13-rate-limiter.md), [Uber](../hld/23-uber.md), [Game Leaderboard](../hld/34-game-leaderboard.md) |
| **Semantic / vector retrieval** | Approximate nearest-neighbour over embeddings, hybrid with keyword search. | **Elasticsearch kNN** / pgvector / a dedicated vector store | Retrieval is similarity, not equality. Hybrid (vector + BM25) beats either alone; pgvector is enough until you outgrow one node. | [RAG Application](../hld/35-rag-application.md) |

Two more that are less "which database" than "the queue *is* the database":

| Use case | Primary store | Why | Seen in |
|---|---|---|---|
| **Work distribution & scheduling** | **Kafka / SQS** + PostgreSQL for job state | The queue provides durability, retry and at-least-once delivery; the database only holds config and terminal status. Exactly-once comes from making the job idempotent, not from the store. | [Job Scheduler](../hld/18-job-scheduler.md), [Notification System](../hld/33-notification-system.md) |
| **Crawl frontier & dedupe** | **Kafka / SQS** + a key-value seen-set (RocksDB, DynamoDB, Cassandra) | Ten billion URL lookups are point reads on a hash — a relational index buys nothing. Front it with a Bloom filter to skip most lookups entirely. | [Web Crawler](../hld/26-web-crawler.md) |

---

## Problem-set database map

Every problem in the [HLD set](../hld/README.md), with the store its design actually leans on.

> Rows marked **°** are stubs in the notebook (title only) — the store there is a
> recommendation, not something the notes committed to. Everything unmarked is either
> stated in the notes or follows directly from them.

### At a glance

| Primary store | Count | Problems |
|---|---:|---|
| **PostgreSQL** (or distributed SQL when multi-region) | 10 | Bit.ly, Dropbox, Yelp°, Local Delivery°, TicketMaster, Leetcode°, Online Auction°, Job Scheduler, Robinhood°, Payment System |
| **Cassandra** (wide-column, append-heavy) | 6 | Instagram, FB News Feed°, WhatsApp, FB Live Comments°, News Aggregator, ChatGPT° |
| **Redis** (ephemeral / hot state) | 5 | Distributed Cache°, Rate Limiter, Google Docs, Online Chess°, Game Leaderboard° |
| **OLAP** (Pinot / Druid / ClickHouse) | 4 | Price Tracking°, YouTube Top K, Ad Click Aggregator, Metrics Monitoring° |
| **S3 + metadata DB** | 2 | YouTube, Strava° |
| **Elasticsearch** | 2 | FB Post Search°, RAG Application |
| **Queue as backbone** (Kafka / SQS) | 3 | Web Crawler, Notification System, Uber |
| **Hybrid, no single owner** | 1 | Tinder° |

The shape worth noticing: **PostgreSQL wins the plurality**, and most of the rest are cases
where the workload is explicitly *not* transactional. That is the opposite of the impression
the vendor-sourced table above gives.

### Problem by problem

| # | Problem | What it is | Primary store | Why that store |
|---:|---|---|---|---|
| 1 | [Bit.ly](../hld/01-bitly-url-shortener.md) | Shorten a URL, redirect on hit, count hits. | **PostgreSQL** + Redis cache | The notes work it out explicitly: 1B rows ≈ 1 TB, so one primary with replicas is enough. Uniqueness of the alias is a `UNIQUE` constraint — free in Postgres, awkward everywhere else. Hits go through the cache to an OLAP table via CDC. |
| 2 | [Dropbox](../hld/02-dropbox.md) | Upload, download, share and sync files across devices. | **S3** (bytes) + **PostgreSQL** (metadata) | The notes split them deliberately. `UserFileAccess` needs indexes on both `file_id` and `user_id` to answer "all files for a user" and "all users for a file" — two access paths over the same rows is a relational job. |
| 3 | [Yelp](../hld/03-yelp.md) ° | Business listings, reviews, search by location. | **PostgreSQL + PostGIS**, **Elasticsearch** for search | Write volume is tiny; the hard parts are radius queries and ranked text search. PostGIS covers geo until scale forces a dedicated index. |
| 4 | [Local Delivery](../hld/04-local-delivery-service.md) ° | Order food/goods, match to a nearby courier. | **PostgreSQL** (orders) + **Redis** geo (couriers) | Same split as Uber: the order is money and needs ACID; courier positions are high-frequency, disposable and belong in memory. |
| 5 | [TicketMaster](../hld/05-ticketmaster.md) | Browse events, hold seats, book without double-selling. | **PostgreSQL** (`SERIALIZABLE`) + **Redis** (HOLD locks) + **Elasticsearch** | The notes ask for a "strong consistent distributed DB". Seat booking is the canonical serializable workload — see [isolation levels](isolation-levels.md#serializable--a-business-rule-is-at-stake). HOLD locks are TTL'd and belong in Redis, not the booking table. |
| 6 | [Instagram](../hld/06-instagram.md) | Post photos, follow users, read a chronological feed. | **Cassandra** (posts) + **Redis** (feed) + S3/CDN | Posts are append-only and partition cleanly by `user_id`. The notes put the feed in a reverse-chronologically sorted Redis structure and use hybrid fan-out to handle celebrities. |
| 7 | [FB News Feed](../hld/07-fb-news-feed.md) ° | Ranked feed of friends' posts. | **Cassandra** + **Redis** | Same shape as Instagram, with ranking replacing chronology. Ranking signals are a separate feature store, not the feed store. |
| 8 | [Tinder](../hld/08-tinder.md) ° | Swipe on nearby profiles, match on mutual likes. | **Cassandra/DynamoDB** (swipes) + **PostgreSQL** (matches) + **Redis** geo | Swipes are enormous, append-only and never updated. A match is a read-check-write on two rows and needs a transaction — so the two halves want different stores. |
| 9 | [Leetcode](../hld/09-leetcode.md) ° | Problems, submissions, sandboxed judging, contests. | **PostgreSQL** + **Redis** (contest leaderboard) + S3 (test cases) | Submission volume is modest — thousands per second at most. The interesting scaling is the judge sandbox pool, not the database. |
| 10 | [WhatsApp](../hld/10-whatsapp.md) | Deliver 1:1 and group messages, sync offline clients. | **Cassandra** (outbox) + **Redis pub/sub** | The notes settle on an outbox partitioned by `user_id`, 30-day retention and per-chat ordering only. Partition key + clustering key + TTL is Cassandra's exact shape. Ordering across chats is explicitly not attempted. |
| 11 | [Strava](../hld/11-strava.md) ° | Record GPS activities, segment leaderboards. | **S3** (raw traces) + **PostgreSQL** (activities) + **Redis** (segments) | A GPS trace is a large immutable blob — store it once, summarise into a row. Segment leaderboards are sorted sets. |
| 12 | [Distributed Cache](../hld/12-distributed-cache.md) ° | Build the cache itself. | **None — you are the store** | In-memory hash map, consistent-hashing ring, LRU eviction, optional replication. The answer is the data structure, not a product. |
| 13 | [Rate Limiter](../hld/13-rate-limiter.md) | Accept or reject per API key against a quota. | **Redis** | The notes make the case: sliding-window timestamps with expiry, sub-10 ms, and *slight inconsistency is tolerable*. Nothing here needs to survive a restart. |
| 14 | [Online Auction](../hld/14-online-auction.md) ° | Accept bids, highest valid bid wins. | **PostgreSQL** (`SERIALIZABLE`) | Concurrent bids on one lot are the write-skew case in miniature — this is the sheet's own "Auction / Bid Processing" row, and it needs serializable isolation, not just row locks. |
| 17 | [YouTube](../hld/17-youtube.md) | Upload, transcode and stream video at multiple bitrates. | **S3 + CDN** + **PostgreSQL/Cassandra** (metadata) | The notes separate `VideoMetaDB` from `ChunkDB`. Bytes dominate the cost by orders of magnitude, so the database question is almost irrelevant — the CDN strategy is the design. |
| 18 | [Job Scheduler](../hld/18-job-scheduler.md) | Register jobs, fire them on time, retry failures. | **PostgreSQL** (config + instances) + **SQS/Kafka** | The hot query is "all instances due before now with status != SUCCESS" — an indexed range scan plus a transactional status transition. Cassandra is a *bad* fit here for exactly that reason. |
| 19 | [FB Live Comments](../hld/19-fb-live-comments.md) ° | Fan out comments on a live stream to millions. | **Cassandra** + **Redis pub/sub** + websockets | Write-heavy, append-only, ordered per stream, read almost entirely as "the last N". Identical shape to WhatsApp. |
| 20 | [News Aggregator](../hld/20-news-aggregator.md) | Scrape publishers, summarise, serve a per-user feed. | **Cassandra** (user feed) + Redis/CDN + S3 | The notes name it: `UserDB — Cassandra`, partition key `location + user_id`. The feed is precomputed, so reads are a single-partition scan. |
| 21 | [Price Tracking](../hld/21-price-tracking-service.md) ° | Poll prices, alert on a drop. | **TimescaleDB / ClickHouse** (history) + PostgreSQL (alert rules) | Price history is append-only time-series with time-range queries — the one workload where a purpose-built time-series store clearly beats a row store. |
| 22 | [YouTube Top K](../hld/22-youtube-top-k.md) | Exact top-K viewed videos per tumbling window. | **Kafka + Flink** → **Pinot/Druid/ClickHouse** + Redis | The notes name all of it, including the caution to avoid a specialised OLAP store if you can. Flink aggregates at minute grain so the serving store never sees raw events; Redis holds the precomputed top-K. |
| 23 | [Uber](../hld/23-uber.md) | Match riders to nearby drivers, prevent double booking. | **Redis** geo (locations) + **PostgreSQL/CockroachDB** (rides) + Temporal | The notes split it exactly this way: write-back cache for the location firehose, ACID distributed DB for the booking, geosharding for latency. Two workloads, two stores. |
| 24 | [Robinhood](../hld/24-robinhood.md) ° | Place trades, maintain an order book and positions. | **In-memory order book** + **PostgreSQL/CockroachDB** (ledger) + Kafka | Matching happens in memory because no database is fast enough; the database records the resulting fills. This is the sheet's financial-ledger row with a matching engine bolted on front. |
| 25 | [Google Docs](../hld/25-google-docs.md) | Real-time collaborative editing with cursors. | **Redis / in-memory** (live doc) + **S3/PostgreSQL** (snapshots) | The notes keep the document in memory and write periodic snapshots. The hot path never touches durable storage; conflict resolution is CRDT/OT or MVCC + OCC, which is an algorithm question, not a database one. |
| 26 | [Web Crawler](../hld/26-web-crawler.md) | Crawl from seed URLs, extract and store text. | **SQS/Kafka** (frontier) + KV seen-set + **S3** (text) | The notes build the whole thing out of queues with retries and a DLQ. Dedupe is a point lookup on a URL hash and a content hash — key-value, at ten-billion scale. |
| 27 | [Ad Click Aggregator](../hld/27-ad-click-aggregator.md) | Ingest clicks, serve advertiser metrics. | **Kafka + Flink** → **OLAP** + S3 archive | Named in the notes: pre-aggregated OLAP tables for sub-second queries, S3 archive so no click is ever truly lost, salted partition keys for hot ads. |
| 28 | [FB Post Search](../hld/28-fb-post-search.md) ° | Full-text search over posts. | **Elasticsearch** | Inverted index with relevance ranking, fed from the post store. The source of truth stays wherever posts live. |
| 29 | [Payment System](../hld/29-payment-system.md) | Stripe-like payment processing with reconciliation. | **PostgreSQL/CockroachDB** + **Kafka** (CDC event log) | The notes demand strong consistency, an idempotency key on `payment_id`, and an immutable Kafka log built from WAL/oplog CDC so the world can be rebuilt. See [core payment platforms](#core-payment-platforms--cockroachdb-or-spanner). |
| 30 | [Metrics Monitoring](../hld/30-metrics-monitoring.md) ° | Ingest metrics, alert, render dashboards. | **Prometheus / VictoriaMetrics / M3** + ClickHouse or S3 for long retention | Metrics are the textbook time-series workload: high-cardinality labels, append-only, queried by time range, downsampled as they age. |
| 31 | [Online Chess](../hld/31-online-chess.md) ° | Matchmaking, live games, ratings. | **Redis** (live game state) + **PostgreSQL** (finished games, Elo) | A game in progress is ephemeral and latency-critical; a finished game is a small immutable record. Moves are an append-only log, replayable to reconstruct any position. |
| 32 | [ChatGPT](../hld/32-chatgpt.md) ° | Conversational LLM serving with history. | **Cassandra/PostgreSQL** (conversations) + **Redis** (streaming state) + vector store (memory) | Conversations partition perfectly by `user_id` and are append-only. The genuinely hard parts are GPU scheduling and token streaming, neither of which is a database problem. |
| 33 | [Notification System](../hld/33-notification-system.md) | Priority email delivery with retries. | **Kafka** (1–10 priority partitions) + **SQS** + PostgreSQL | The notes make the queue the design: a partition per priority so low-priority traffic can be skipped without starving, and at-least-once by deleting from SQS only after send. |
| 34 | [Game Leaderboard](../hld/34-game-leaderboard.md) ° | Global and windowed score rankings. | **Redis** sorted sets + Cassandra/PostgreSQL for durability | `ZADD`/`ZREVRANGE` is O(log n) and gives rank directly — see [sorted sets](hld.md#sorted-sets) in the HLD appendix. Redis is the serving layer; truth lives behind it. |
| 35 | [RAG Application](../hld/35-rag-application.md) | Ingest documents, retrieve chunks, generate answers. | **Elasticsearch** (hybrid kNN + BM25) + **PostgreSQL** (doc/chunk status) | The notes build an ES index over embedded chunks and track per-document ingestion status separately. Hybrid retrieval beats pure vector search; the status table is what makes ingestion resumable. |

### Standalone designs

| Design | What it is | Primary store | Why |
|---|---|---|---|
| [Revenue Recognition](../revenue_recognition_pipeline.md) | Mongo oplog → Flink → SQS → Iceberg → Pinot/DataStory. | **Iceberg** (lakehouse) + **Pinot** (serving) | Iceberg gives upserts and time-travel over object storage for the hourly Spark layer; Pinot serves the dashboard at sub-second latency. Two stores because batch correctness and query latency are different jobs. |
| [Database Version Control](../database_version_control.md) | Branch, diff, validate and commit a schema like git. | **PostgreSQL** (ChangelogDB) + ephemeral per-branch DBs | Commits, content hashes and schema snapshots are small structured records with strict auditability needs. Each branch's `TmpDB` is a throwaway container, not storage. |

### LLD problems

The [LLD set](../lld/README.md) is class-design, so most problems have no database at all —
[Connect Four](../lld/06-connect-four.md), [Elevator](../lld/08-elevator.md),
[Parking Lot](../lld/09-parking-lot.md), [File System](../lld/10-file-system.md) and
[Spreadsheet](../lld/16-spreadsheet-with-formulas.md) are in-memory object models where the
interesting question is the data structure, not the store. Four do imply persistence:

| Problem | Implied store | Why |
|---|---|---|
| [Payment Wallet](../lld/03-payment-wallet.md) | **PostgreSQL**, `SERIALIZABLE` | Debit and credit in one transaction — the ledger row from the sheet, at single-service scale. |
| [Inventory Management](../lld/14-inventory-management.md) | **PostgreSQL** with row locks | Same hot-row contention as the order-management row above, without the multi-region part. |
| [Movie Ticket Booking](../lld/11-movie-ticket-booking.md) | **PostgreSQL**, `SERIALIZABLE` | Seat booking again — the LLD counterpart of TicketMaster. |
| [Logging Service](../lld/12-logging-service.md) | Append-only file → **S3/OLAP** | Writes are sequential and never updated; reads are time-range scans. |

---

## The candidates

| Database | Model | Default isolation | Replication | Picks up when |
|---|---|---|---|---|
| **PostgreSQL** | Single-primary relational | Read Committed | Async (sync optional) | One region, one primary is enough — the default until proven otherwise. |
| **PostgreSQL + Citus** | Sharded relational | Read Committed | Async | Scale-out reads/writes with Postgres semantics, still one region. |
| **Spanner** | Distributed SQL | Serializable (externally consistent) | Synchronous Paxos | You're on GCP and want the strongest guarantee that exists. |
| **CockroachDB** | Distributed SQL, Postgres wire-compatible | Serializable | Synchronous Raft | Multi-region writes, any cloud, data residency, online schema changes. |
| **YugabyteDB** | Distributed SQL, Postgres wire-compatible | Snapshot (Serializable available) | Synchronous Raft | Same shape as CockroachDB; deeper Postgres feature reuse. |
| **TiDB** | Distributed SQL, MySQL wire-compatible | Snapshot (Repeatable Read) | Synchronous Raft | You're a MySQL shop and want HTAP in the same cluster. |
| **DynamoDB** | Managed key-value | — (transactions scoped, single region) | Sync in-region, async Global Tables | Predictable key-based access at any scale, no ops. |
| **Cassandra** | Wide-column, leaderless | Tunable (LWW conflicts) | Quorum, tunable | Write-heavy, append-only, partition-key access; not a ledger. |
| **Redis** | In-memory | — | Async | Sessions, counters, leaderboards, live state; not durable truth. |
| **Elasticsearch** | Inverted index + kNN | — (near-real-time index) | Sync replica shards | Ranked full-text, faceting, geo, hybrid vector search — always a derived index. |
| **Pinot / Druid / ClickHouse** | Columnar OLAP | — | Segment replication | Pre-aggregated analytics: top-K, funnels, dashboards over billions of rows. |
| **Prometheus / VictoriaMetrics** | Time-series | — | Varies | Metrics with high-cardinality labels, time-range queries, downsampling. |
| **Iceberg on S3** | Lakehouse table format | Snapshot isolation | Object-store durability | Batch upserts, time travel, schema evolution over cheap storage. |
| **Kafka** | Durable log | — (ordered per partition) | Sync ISR replication | The system of record for events; replay, fan-out, and back-pressure. |
| **S3** | Object storage | — | 11 nines durability | Bytes: media, backups, archives, lakehouse files. Never file bytes in a DB. |

## Key design per store

Picking the database is the easy half. The half that decides whether the design survives
production is **which columns become the key** — because in almost every store the key is
not just a uniqueness constraint, it is the *physical layout*: which node the row lands on,
which rows are read together, and which query is a single seek versus a fan-out to every
shard in the cluster.

The vocabulary collides across stores, which is most of why this is confusing. Underneath,
every store is asking at most three questions:

| | Question | Called | Get it wrong and |
|---|---|---|---|
| **1. Identity** | What makes this row unique? | primary key, `_id`, document key | duplicates appear silently (stores without uniqueness), or writes upsert over each other |
| **2. Placement** | Which node / file / segment does it live on? | partition key, shard key, distribution column, routing, hash key | one node takes all the traffic, or every query fans out to all of them |
| **3. Order** | How are co-located rows sorted on disk? | clustering key, sort key, `ORDER BY`, sorted column | "last N" becomes a full partition scan and compression collapses |

A single-node PostgreSQL lets you ignore #2 entirely, which is why it is the default —
and why sharding it later is the hardest retrofit in the system. Cassandra and DynamoDB
force you to answer all three before you can create a table, which feels hostile and is
actually the honest version.

### Vocabulary map

| Store | Identity | Placement (co-location) | Order within placement |
|---|---|---|---|
| **PostgreSQL** (single node) | `PRIMARY KEY` | — none, one node | heap is unordered; index order only |
| **PostgreSQL** (declarative partitioning) | PK **must contain** the partition key | `PARTITION BY RANGE/LIST/HASH` | per-partition indexes |
| **Citus** | PK must include the distribution column | `create_distributed_table(t,'tenant_id')` | per-shard indexes |
| **CockroachDB / YugabyteDB** | `PRIMARY KEY` — *is* the physical KV sort order | leading PK columns → range/tablet split; `REGIONAL BY ROW` prepends `crdb_region` | trailing PK columns |
| **Spanner** | `PRIMARY KEY`, sorted | leading PK column + `INTERLEAVE IN PARENT` | trailing PK columns |
| **TiDB** | clustered PK, or hidden `_tidb_rowid` | region splits by key range; `AUTO_RANDOM` / `SHARD_ROW_ID_BITS` | PK order |
| **MySQL + Vitess** | clustered PK (secondary indexes carry the PK) | **vindex** on the sharding column | PK order |
| **DynamoDB** | partition key, or partition + sort key | `hash(partition key)` | sort key |
| **Cassandra** | `PRIMARY KEY ((partition), clustering…)` — no uniqueness enforcement | `token(partition key)` on the ring | clustering columns, `CLUSTERING ORDER BY` |
| **MongoDB** | `_id` (globally unique only if it is the shard key prefix) | shard key, hashed or ranged | index order |
| **Redis** | the key string | `CRC16(key) mod 16384` → slot; `{hashtag}` overrides | ZSET score, or lexicographic |
| **Kafka** | none — offset within a partition | `murmur2(key) % partitions` | append order (offset) |
| **Elasticsearch** | `_id` | `hash(_routing ?? _id) % primary_shards` | none by default; `index.sort.field` optional |
| **Pinot** | none, unless it's an upsert table (then a primary key) | segment partitioning column | one `sortedColumn` per segment |
| **ClickHouse** | `ORDER BY` tuple (`PRIMARY KEY` is a sparse prefix of it) | `PARTITION BY` on disk; sharding key on the `Distributed` table | `ORDER BY` |
| **Druid** | none, unless rolling up (then the dimension tuple) | time chunk, then secondary `partitionsSpec` | time, then dimension order |
| **Iceberg** | identifier fields (for MOR upserts) | `PARTITIONED BY days(ts), bucket(64, id)` — hidden | write sort order |
| **Prometheus** | series = metric name + full label set | series hash | timestamp, by construction |
| **S3** | object key | key prefix | lexicographic listing |

---

### PostgreSQL — the key is logical until it isn't

- **Primary key.** Prefer `bigint GENERATED ALWAYS AS IDENTITY`, or **UUIDv7 / ULID** when
  IDs must be generated client-side. Do *not* make random **UUIDv4** the PK of a large
  table: inserts scatter across the whole B-tree, so the working set of index pages never
  fits cache, and every dirtied page triggers a full-page write into the WAL. UUIDv7 is
  time-ordered, so inserts stay append-like and keep the leaf-page locality. PostgreSQL 18
  ships `uuidv7()` natively.
- **Natural keys are fine when they're stable and narrow.** [Bit.ly](../hld/01-bitly-url-shortener.md)
  keys on `short_code` — a `UNIQUE` constraint *is* the collision check, so there is no
  read-then-write race to reason about.
- **The heap is unordered.** Index order is logical; `CLUSTER` is a one-shot rewrite that
  decays. BRIN indexes only pay off on naturally correlated columns — an append-only
  `created_at` — where they cost a few kilobytes instead of gigabytes.
- **Composite index column order:** equality predicates first, the range predicate last,
  because the scan stops being selective after the first range. `(status, next_run_at)`
  for the [Job Scheduler](../hld/18-job-scheduler.md)'s "due and not yet succeeded" query,
  ideally as a partial index `WHERE status <> 'SUCCESS'` so completed history stays out of
  the index entirely.
- **Partitioning is for retention, not usually for speed.** `DROP PARTITION` is instant
  where `DELETE` of a month of rows is hours of vacuum. The catch: the PK must contain the
  partition key, and pruning only happens when the partition key is in the `WHERE` clause —
  a query that filters only on `user_id` when you partitioned by month touches every
  partition.
- **Citus:** pick **one** distribution column and use it across every co-located table
  (`tenant_id` is the canonical choice). Joins and foreign keys work only within the same
  distribution column; small dimension tables become reference tables replicated to every
  node. The moment two tables need different distribution columns, one of them is going to
  be scatter-gather forever.

### Distributed SQL (CockroachDB, Spanner, YugabyteDB, TiDB) — the PK is the shard map

These stores keep rows **sorted by primary key** in an underlying KV, and split contiguous
key ranges into ranges/tablets owned by one leaseholder. That single fact drives every key
decision:

- **A monotonic PK is a single-node write hotspot.** `SERIAL`, `AUTO_INCREMENT`, `now()`,
  a Snowflake ID — every insert targets the last range, so a 30-node cluster writes at the
  speed of one node. Fixes, in rough order of preference:
  - make the PK's leading column something naturally spread (`user_id`, `account_id`);
  - prepend a computed bucket: `PRIMARY KEY (crc32(id) % 16, id)`;
  - CockroachDB `CREATE INDEX … USING HASH WITH (bucket_count = 8)`;
  - TiDB `AUTO_RANDOM` on the clustered PK, or `SHARD_ROW_ID_BITS` when there isn't one;
  - Spanner: hash or bit-reverse the key before storing it.
- **But randomising costs you range scans.** The two goals fight. The resolution is almost
  always a **composite PK: spread on the leading column, order on the trailing one** —
  `PRIMARY KEY (customer_id, created_at DESC, id)` spreads writes across customers while
  keeping "this customer's latest 50" one contiguous seek.
- **Co-location for transactions.** A transaction touching two rows in the same range is
  one Raft round; across two ranges it is 2PC. Spanner's `INTERLEAVE IN PARENT` physically
  nests child rows under the parent; YugabyteDB has colocated tables and tablegroups;
  CockroachDB dropped interleaving in v21.2, so you get the same effect by giving child
  tables a PK that shares the parent's prefix. For the
  [Payment System](../hld/29-payment-system.md), `payments` and `payment_events` sharing a
  `payment_id` prefix is what keeps the write path a single-range commit.
- **Every secondary index is another Raft group.** An index lives in its own ranges, so an
  insert into a table with three indexes is a distributed transaction over four ranges.
  This is the main reason distributed SQL write throughput disappoints people migrating
  from PostgreSQL — the schema came along unchanged.
- **Residency is a key decision.** CockroachDB's `REGIONAL BY ROW` prepends a hidden
  `crdb_region` column *to the primary index*, so you must be able to derive the region at
  write time (from the user's home region, not from where the request landed). Rows whose
  region you can't compute end up in the wrong place and the compliance argument evaporates.

### DynamoDB — you get one access path per key, so design them all up front

- **`PK` alone** for pure point lookups; **`PK` + `SK`** whenever you need "all the X for a
  Y" — the sort key turns a partition into a range-scannable collection.
- **Hierarchical sort keys** are the trick that makes one table serve many queries:
  `SK = "ORG#123#DEPT#7#USER#9"` answers three prefix queries with `begins_with`.
  Overloaded GSI keys (`GSI1PK`/`GSI1SK` holding different meanings per item type) are the
  single-table-design version of the same idea.
- **Hard limits shape the key.** ~3,000 RCU / 1,000 WCU per physical partition, and a 10 GB
  cap on an item collection once the table has an LSI. Adaptive capacity smooths bursts; it
  does not save you from one genuinely hot key.
- **Write sharding for hot keys:** store `PK = "ad#42#" + rand(0..N)` and scatter-gather N
  reads. This is the same fix as salting a Kafka key, and it is what the
  [Ad Click Aggregator](../hld/27-ad-click-aggregator.md) needs for a viral ad.
- **GSI vs LSI:** a GSI is a separate table with its own partitioning and its own
  throughput, always eventually consistent — and if it throttles, it back-pressures writes
  to the *base* table. An LSI shares the partition, can be read strongly consistently, must
  be declared at table creation, and is what imposes the 10 GB collection cap.
- **TTL** is a first-class expiry attribute; use it rather than a delete sweeper.

### Cassandra — `PRIMARY KEY ((partition), clustering…)`

The one store where getting the key wrong doesn't degrade the system, it *breaks* it.

- **Model the query, not the entity.** One table per access pattern; every read should hit
  exactly one partition. Denormalise freely — see
  [query-driven data model](hld.md#data-model-aka-query-driven).
- **Bound the partition.** Target ≲100 MB and ≲100k rows, and more importantly make it
  *bounded in time*, because an unbounded partition grows until compaction and repair can't
  finish. The fix is a bucket in the partition key:
  `PRIMARY KEY ((chat_id, day), created_at, message_id)`. Choose the bucket width from the
  write rate, and remember the read side must now query N buckets to cover a range.
- **Low-cardinality partition keys are hot nodes.** `PARTITION BY country` puts a third of
  the traffic on whichever node owns `US`.
- **Clustering columns give you ordering for free:**
  `WITH CLUSTERING ORDER BY (created_at DESC)` makes "the last 50 messages" a head scan
  with no sort. This is why [WhatsApp](../hld/10-whatsapp.md) and
  [FB Live Comments](../hld/19-fb-live-comments.md) fit Cassandra so exactly.
- **There is no uniqueness constraint.** Two writes with the same primary key are an
  upsert, last-write-wins by timestamp — so the PK must include enough columns to be
  genuinely unique (append `message_id`, don't trust `created_at`), and anything requiring
  a real uniqueness check needs `IF NOT EXISTS`, which costs a Paxos round.
- **Don't use it as a queue.** Delete-heavy partitions accumulate tombstones that are
  scanned on every read until `gc_grace_seconds` (default 10 days) passes and compaction
  runs. TTLs are the supported way to expire data — [WhatsApp](../hld/10-whatsapp.md)'s
  30-day retention is a TTL, not a delete job.
- **Secondary indexes are per-node scatter-gather.** Write a second denormalised table
  instead; that is the whole idiom.
- **Static columns** hold per-partition metadata (a chat's title) without duplicating it on
  every row.

### MongoDB — the shard key is close to permanent

- **Choose for cardinality, frequency and monotonicity**: high cardinality, no single
  dominant value, and *not* increasing. `ObjectId` and timestamps are monotonic, so a
  ranged shard key on them sends every insert to the max-key chunk.
- **Hashed vs ranged:** hashed spreads writes perfectly and destroys range queries and
  targeted routing; ranged preserves locality and risks the hot chunk. The usual answer is
  a **compound ranged key** — `{tenant_id: 1, created_at: 1}` — which spreads by tenant and
  keeps a tenant's time range contiguous.
- **A query without the shard key is a scatter-gather** to every shard, plus a merge on
  mongos. Every access pattern you care about should carry the key.
- **Unique indexes must be prefixed by the shard key**, so global uniqueness on any other
  field is not available once sharded. Resharding exists since 5.0 but it rewrites the
  collection — treat the choice as a one-way door.
- The **oplog** is a capped collection ordered by timestamp; that ordering is what makes
  CDC tailing work in the [Revenue Recognition pipeline](../revenue_recognition_pipeline.md).

### Redis — the key string is the shard key

- **Cluster routing is `CRC16(key) mod 16384`.** Multi-key operations (`MGET`,
  transactions, Lua scripts) require every key in the same slot, so co-locate deliberately
  with hash tags: `{user:123}:profile` and `{user:123}:feed` hash on `user:123` only.
- **Name keys as a path** — `app:entity:id:field` — and keep them short; a hundred million
  keys carrying 30 wasted bytes each is 3 GB of RAM spent on strings.
- **Beware the single big key.** A leaderboard `ZSET` with 50 M members lives in one slot
  on one node, and any O(N) command against it blocks the single-threaded event loop for
  everyone. Shard [game leaderboards](../hld/34-game-leaderboard.md) by time window or score
  band and merge the top-K in the application.
- **The ZSET score is your sort key.** Pack a tie-breaker into the float (score × 2^20
  minus a timestamp) when equal scores must order deterministically, or use
  `ZRANGEBYLEX` with equal scores and an encoded member.
- **Every ephemeral key needs a TTL.** Without one, the eviction policy makes the decision
  for you — and `noeviction` turns a full instance into write errors. Rate-limit counters,
  HOLD locks and session tokens should all expire on their own.

### Kafka — the partition key is an ordering contract

- **Ordering exists only within a partition**, so the key must be exactly the entity whose
  order matters: `chat_id` for messages, `account_id` for ledger events, `payment_id` for a
  payment's lifecycle. Keying on something coarser (`region`) buys ordering you don't need
  and throughput you can't get back.
- **Partition count is close to immutable.** Since the mapping is `hash(key) % partitions`,
  adding partitions reshuffles every key and breaks per-key ordering across the change.
  You cannot decrease at all. Over-provision at creation.
- **A hot key is a hot partition** and there is no way around it except changing the key.
  For [ad clicks](../hld/27-ad-click-aggregator.md), salt it — `ad_id#rand(0..N)` — and
  re-aggregate downstream, which is fine precisely because counting is commutative.
- **Consumer parallelism ≤ partition count.** The partition key therefore also sets your
  maximum scale-out.
- **On a compacted topic the key is the identity of the record** — the last value per key
  survives, which turns the topic into a durable KV snapshot (and means a null key breaks
  compaction).

### Elasticsearch — routing decides fan-out

- **`hash(_routing ?? _id) % number_of_primary_shards`.** The primary shard count is baked
  into the modulus, which is why it is fixed at index creation and only changeable by
  split/shrink/reindex.
- **Custom routing is the biggest available latency win** for multi-tenant search: routing
  by `tenant_id` turns a query that visits 20 shards into one that visits one. The cost is
  skew — one enormous tenant fills one shard — so large tenants usually get an index of
  their own.
- **Time-based indices are the partition key for logs.** Roll over daily via a data stream
  and ILM, then delete whole indices; deleting documents by query is far more expensive.
- **`index.sort.field`** lets a "top N by timestamp" query terminate early instead of
  scoring the whole segment.
- **Parent-join requires routing** to keep parent and child on one shard — which is really
  a restatement of the rule that Elasticsearch cannot join across shards.

### OLAP: Pinot, Druid, ClickHouse

All three trade the same way: the sort key gives you both pruning *and* compression,
because sorted columns of low-cardinality values compress to almost nothing.

- **ClickHouse.** `ORDER BY` is the real key — the sparse primary index is a prefix of it.
  Order **lowest-cardinality and most-filtered first, highest-cardinality last**:
  `ORDER BY (tenant_id, event_type, ts)`. `PARTITION BY` should be *coarse*
  (`toYYYYMM(ts)`); each insert creates at least one part per partition, so fine-grained
  partitioning produces a merge storm and "too many parts" errors. On a `Distributed` table
  the sharding key should be `cityHash64(tenant_id)`, so each tenant's aggregation finishes
  locally instead of shuffling.
- **Pinot.** Pick the **partition column** that appears in every query's `WHERE`
  (`ad_id`, `video_id`) so the broker can prune segments; declare one **`sortedColumn`**
  per segment for range scans and compression; the **time column** drives segment pruning
  and retention. Star-tree indexes pre-aggregate the dimension combinations you actually
  group by. Upsert tables need a primary key *and* the upstream Kafka topic partitioned by
  that same key — Pinot resolves upserts per partition, so a mismatched partitioning
  silently produces duplicates. See [Pinot architecture](hld.md#pinot-architecture) and the
  [YouTube Top-K](../hld/22-youtube-top-k.md) design.
- **Druid.** Segment granularity is a time bucket first; secondary partitioning
  (`partitionsSpec`, hashed or ranged on a dimension) is what stops one hour from becoming
  one giant segment. When rollup is on, the **dimension tuple is the key** — rows sharing
  it collapse — so including a high-cardinality dimension like `request_id` disables the
  compression you turned rollup on for.

### Iceberg — hidden partitioning, and the small-file trap

- **`PARTITIONED BY days(event_ts), bucket(64, account_id)`.** Partitioning is hidden:
  queries filter on `event_ts` and Iceberg derives the partition, so you never add a
  synthetic `dt` column and never depend on callers remembering to filter on it.
- **Bucket the key you merge on.** `MERGE INTO … ON t.account_id = s.account_id` only has
  to rewrite the buckets that were touched, which is what keeps an hourly upsert job
  bounded — the mechanism behind the
  [Revenue Recognition pipeline](../revenue_recognition_pipeline.md)'s Iceberg layer.
- **Sort order inside files** drives min/max column statistics, and therefore file pruning.
- **Partition evolution is supported** — old data keeps the old spec — which is the main
  reason to prefer Iceberg over Hive-style directory partitioning.
- **Over-partitioning is the classic failure:** hourly × a high-cardinality column produces
  millions of tiny files, and query planning starts costing more than scanning. Target
  file sizes in the hundreds of megabytes and compact.

### Prometheus and time-series — cardinality *is* the key

- A **series is the metric name plus the full label set**; each unique combination gets its
  own memory and index entry. Every label multiplies: 1 label with 10k values times another
  with 100 is a million series.
- **Never put an unbounded value in a label** — `user_id`, `request_id`, email, full URL
  path. That is the single failure mode that takes down monitoring, and it happens in
  [metrics systems](../hld/30-metrics-monitoring.md) far more often than any storage issue.
- Timestamp is the clustering key by construction; retention and downsampling are the only
  other knobs.

### S3 — the key prefix is the partition

- Prefixes auto-scale (~3,500 write / 5,500 read requests per second per prefix), so
  random hash prefixes are no longer needed for throughput — that advice is a decade out of
  date.
- What the key still buys you is **listing and pruning**:
  `table/dt=2026-08-28/hour=03/part-0001.parquet` makes a day's data one prefix scan and
  lets every query engine skip the rest. Listing is lexicographic, so design the key so
  that the common query is a prefix.

---

### Anti-patterns, across every store

| Symptom | Cause | Fix |
|---|---|---|
| One node at 100% while the cluster idles | monotonic key on a range-sharded store (CRDB, Spanner, TiDB, ranged Mongo) | composite PK with a spread leading column, or a hash bucket prefix |
| Write throughput collapses as the table grows | random UUIDv4 PK — no insert locality, WAL/compaction amplification | UUIDv7 / ULID, or a `bigint` identity |
| One partition owns a third of the data | low-cardinality partition key (`country`, `status`, `tenant` in a whale-heavy tenancy) | add a discriminating column, or give the whale its own index/table |
| Reads get slower every week, repairs never finish | unbounded partition (Cassandra), unbounded item collection (Dynamo) | bucket the partition key by time; TTL the tail |
| Every query touches every shard | the access pattern doesn't include the placement key | a second denormalised table, a GSI, or custom routing — not a secondary index |
| Duplicate rows nobody can explain | assuming uniqueness on a store that doesn't enforce it (Cassandra, Pinot without upserts, ES `_id` collisions) | put real uniqueness in the key, or enforce it upstream in a store that has constraints |
| "Too many parts" / query planning dominates runtime | over-partitioning (ClickHouse `PARTITION BY` day, Iceberg hourly × high cardinality) | coarsen the partition, compact aggressively |
| Writes are slower than the benchmark promised | one Raft group per secondary index on distributed SQL | drop indexes you don't query; cover the query with the PK order instead |
| Monitoring falls over before the service does | unbounded label values in a time-series store | move the high-cardinality dimension to logs or an OLAP store |

### Answering this in an interview

Four sentences, in this order, works for any store:

1. **The access pattern** — "the dominant read is *the last 50 messages in a chat*".
2. **The placement key** — "so I partition on `chat_id`, bucketed by day to bound the
   partition".
3. **The order key** — "clustered by `created_at DESC` so the read is a head scan, no sort".
4. **The failure mode you're avoiding** — "and I'm not partitioning on `user_id` alone
   because a group chat with a million members would be an unbounded partition".

Naming the failure mode is what separates a memorised schema from a designed one.

### Worked keys from the problem set

| Problem | Store | Key | Reasoning |
|---|---|---|---|
| [Bit.ly](../hld/01-bitly-url-shortener.md) | PostgreSQL | PK `short_code` | the uniqueness constraint *is* the collision check; lookups are point reads on the PK |
| [Dropbox](../hld/02-dropbox.md) | PostgreSQL | PK `(file_id, user_id)` on `UserFileAccess` + index on `(user_id, file_id)` | two access paths — "users of a file" and "files of a user" — need two orderings of the same pair |
| [TicketMaster](../hld/05-ticketmaster.md) | PostgreSQL + Redis | PK `(event_id, seat_id)`; Redis `hold:{event_id}:{seat_id}` with TTL | booking rows co-locate per event; the hold is an expiring key, deliberately not a row |
| [Instagram](../hld/06-instagram.md) | Cassandra | `((user_id, month), created_at DESC, post_id)` | one partition per user-month keeps it bounded; feed reads are a head scan |
| [WhatsApp](../hld/10-whatsapp.md) | Cassandra | `((user_id, day), created_at DESC, message_id)`, TTL 30d | per-user outbox, ordering only within a chat, retention as a TTL rather than deletes |
| [Job Scheduler](../hld/18-job-scheduler.md) | PostgreSQL | PK `(job_id, run_at)`; partial index `(next_run_at) WHERE status <> 'SUCCESS'` | the hot query is a range scan over pending work; completed history stays out of the index |
| [News Aggregator](../hld/20-news-aggregator.md) | Cassandra | `((location, user_id), rank)` | the notes' own key — the feed is precomputed, so a read is one partition, already ordered |
| [YouTube Top-K](../hld/22-youtube-top-k.md) | Kafka → Pinot | Kafka key `video_id`; Pinot partition `video_id`, sorted column `window_start` | per-video ordering into Flink; segment pruning by video on the serving side |
| [Uber](../hld/23-uber.md) | Redis + distributed SQL | Redis geohash cell as key; rides PK `(rider_id, ride_id)` | locations shard by geography because queries are "who's near me"; rides shard by rider |
| [Ad Click Aggregator](../hld/27-ad-click-aggregator.md) | Kafka → OLAP | Kafka key `ad_id#rand(0..N)`; OLAP `ORDER BY (ad_id, minute)` | salting because a viral ad is a hot partition, and counting is commutative so it re-merges |
| [Payment System](../hld/29-payment-system.md) | Distributed SQL | PK `(payment_id)`, events PK `(payment_id, seq)`; idempotency key unique index | sharing the prefix keeps payment and its events in one range → one-round commit |
| [Game Leaderboard](../hld/34-game-leaderboard.md) | Redis | `lb:{game_id}:{window}` ZSET, score = points | window in the key so an expiring leaderboard is a `DEL`, and no key grows forever |


---

## When you don't need distributed SQL

Distributed SQL costs you: cross-region commit latency on every write, a much larger
operational surface, and a licence bill. Skip it when

- you run in **one region** — a PostgreSQL primary with a synchronous standby gives ACID
  and `RPO = 0` inside that region;
- your data **partitions cleanly** and you never need one atomic transaction across two
  partitions — shard it, or use Vitess;
- your working set fits comfortably on one large machine (modern instances go to
  hundreds of vCPUs and terabytes of RAM — this covers more businesses than people think);
- you only need **global reads**, not global writes — one primary plus regional read
  replicas is far cheaper than global consensus.

The honest interview line: reach for distributed SQL when you need **multi-region writes
with strong consistency**, or when **data residency law** forces rows to live in specific
countries. Everything else is usually PostgreSQL.

## Related

- [Isolation levels](isolation-levels.md) — which level each of these workloads actually needs
- [HLD appendix](hld.md) — consistency models, Elasticsearch, Cassandra, Kafka, Redis, Iceberg, Pinot
- [HLD problems](../hld/README.md)
