# Scaling Strategies — Caching, Sharding, Load Balancing

[← Appendix index](README.md) · [All docs](../README.md)

*Not from the cheat-sheet spreadsheet — assembled from the patterns the
[HLD problem set](../hld/README.md) keeps reaching for.*

Once the functional design is drawn, [the NFR pass](../hld/00-interview-framework.md#nfr-checklist-scale-for-cloud-designs)
comes down to three levers, and they are the same three every time:

| Lever | What it does | What it costs |
|---|---|---|
| **Caching** | removes work — the same answer isn't computed twice | staleness, and a whole class of failure modes that only appear under load |
| **Sharding** | splits work — no single node holds everything | cross-shard joins, transactions, ordering and uniqueness all become your problem |
| **Load balancing** | places work — no single instance takes everything | statelessness, and a health-check story that can take the system down on its own |

The marks in an interview are not in naming the pattern. They are in naming **what the
pattern costs and how you contain it** — so each section below ends with the failure modes.

- [A) Caching](#a-caching) — [layers](#where-caches-live) · [read](#read-strategies) and [write](#write-strategies) patterns · [invalidation](#invalidation--the-hard-half) · [eviction](#eviction) · [the four failure modes](#the-failure-modes-that-only-appear-under-load) · [consistency](#read-after-write-consistency)
- [B) Sharding](#b-sharding) — [do you need it](#first-do-you-actually-need-to-shard) · [strategies](#the-six-strategies) · [consistent hashing](#consistent-hashing-in-detail) · [resharding](#resharding-without-downtime) · [routing](#routing-who-knows-where-the-data-is) · [what it breaks](#what-sharding-breaks)
- [C) Load balancing](#c-load-balancing) — [layers](#the-layers) · [algorithms](#the-algorithms) · [stickiness & websockets](#session-affinity-and-the-websocket-problem) · [health checks](#health-checks-and-how-they-cause-outages) · [overload protection](#overload-protection) · [multi-region](#multi-region-traffic)
- [D) Scaling triggers](#d-scaling-triggers--when-one-machine-is-actually-done) — [the reference machine](#the-reference-machine) · [what it serves](#what-the-reference-box-actually-serves) · [the utilisation knee](#the-rule-behind-every-threshold-the-utilisation-knee) · [which signal to scale on](#which-signal-to-scale-on) · [the concurrency trigger](#the-concurrency-trigger-concretely) · [TPS ceilings](#tps-ceilings-and-what-pooling-actually-buys) · [per-resource](#per-resource-triggers-on-the-reference-box) and [database](#database-triggers-on-the-same-box) triggers · [cloud-era misses](#cloud-era-triggers-people-miss) · [lead time](#trigger-lead-time--the-formula-that-sets-the-threshold) · [anti-triggers](#anti-triggers)
- [E) The escalation ladder](#e-the-escalation-ladder)
- [F) Problem-set map](#f-problem-set-map)

---

## A) Caching

### Where caches live

Every layer between the user and the disk can hold a copy. Cheapest wins are furthest from
the database.

| Layer | Typical tech | Holds | Invalidation | Notes |
|---|---|---|---|---|
| **Client / browser** | `Cache-Control`, `ETag` | static assets, GET responses | TTL, revalidation | free — costs you nothing to serve, and you cannot recall it |
| **CDN / edge** | CloudFront, Fastly, Cloudflare | media, static, cacheable API GETs | TTL + explicit purge | the single biggest lever for read-heavy media — [YouTube](../hld/17-youtube.md), [Dropbox](../hld/02-dropbox.md) |
| **Reverse proxy** | nginx, Varnish, Envoy | full HTTP responses | TTL + purge | dedupes identical concurrent requests for free |
| **In-process (L1)** | Caffeine, Guava, `sync.Map` | hot config, feature flags, tiny hot set | TTL only, realistically | zero network hop; N instances means N divergent copies, so only for data where seconds of skew are fine |
| **Distributed (L2)** | Redis, Memcached | sessions, objects, counters, precomputed lists | explicit delete + TTL | one shared truth, one network hop, one more thing to run |
| **Materialised read model** | Redis lists/ZSETs, a denormalised table | feeds, leaderboards, top-K | rebuilt by a pipeline | not really a cache — a derived store you can rebuild — [Instagram](../hld/06-instagram.md), [YouTube Top-K](../hld/22-youtube-top-k.md) |
| **Database buffer pool** | Postgres `shared_buffers`, InnoDB pool | pages | LRU, automatic | the cache you already have; check the hit ratio before adding another tier |

**L1 + L2 together** is the standard answer for a genuinely hot key: a 1-second in-process
cache in front of Redis absorbs the fan-out, and Redis absorbs the fan-out to the database.
The cost is that your staleness budget is now the sum of both TTLs.

### Read strategies

| Pattern | Flow | Who populates | Use when | Watch out for |
|---|---|---|---|---|
| **Cache-aside** (lazy) | app checks cache → miss → read DB → write cache | the application | the default; works with any store | every miss hits the DB, so a cold start or a mass expiry is a stampede |
| **Read-through** | app asks the cache; the cache loads on miss | the cache library | you want the loader logic in one place | same miss behaviour, hidden behind an abstraction |
| **Refresh-ahead** | refresh a hot key *before* its TTL expires | a background loader | small, known-hot key set (config, top-K) | wasted refreshes on keys that were about to go cold |

Cache-aside is what almost every design here means when it says "add a cache". Its one real
weakness is the miss path, which is where the failure modes below live.

### Write strategies

| Pattern | Write path | Read-after-write | Durability | Use when |
|---|---|---|---|---|
| **Write-through** | write cache **and** DB synchronously | consistent | full — DB is written | correctness matters and writes are rare relative to reads |
| **Write-around** | write DB only, **delete** the cache key | consistent after the delete | full | write-heavy data that is rarely read back; keeps the cache from filling with cold entries |
| **Write-back** (write-behind) | write cache, flush to DB asynchronously | consistent | **lossy** — a node death loses the unflushed tail | the write rate exceeds what the DB can take *and* the data is reconstructible |

**Write-back is the interesting one.** [Uber](../hld/23-uber.md)'s notes name it for driver
location updates, and the reason is precise: a location that is 5 seconds stale is
worthless anyway, and a lost one is replaced by the next ping 4 seconds later. Write-back
is correct there and catastrophic for a payment. The test is not "is it fast" — it is
**"can I regenerate what I lose?"**

### Invalidation — the hard half

| Approach | How | Staleness | Good for |
|---|---|---|---|
| **TTL only** | let it expire | bounded by the TTL | anything where a known staleness window is acceptable — most things |
| **Delete on write** | write DB, then `DEL` the key | near-zero, with a race window | the default companion to cache-aside |
| **Versioned keys** | key includes a version: `user:123:v7` | zero | avoids deletes entirely; old versions age out on their own |
| **CDC-driven** | WAL/oplog → stream → invalidator | seconds | the only one that catches writes made **outside** your application — migrations, admin scripts, another service |

Three rules that cover most of it:

1. **Delete, don't update.** Two concurrent writers that each *update* the cache can land
   in the opposite order they hit the database, leaving the cache permanently wrong. A
   delete just forces the next reader to re-read.
2. **Delete after commit, not before.** Deleting first lets a concurrent reader repopulate
   the cache from the old, still-uncommitted state.
3. **The remaining race is unavoidable** — a reader can load the old value and write it to
   the cache after your delete. If that matters, use a short TTL as a backstop, or
   delete-wait-delete (delete, sleep past the read window, delete again). If it *really* matters, don't cache it.

### Eviction

| Policy | Keeps | Best for |
|---|---|---|
| **LRU** | recently used | general purpose; the default and usually right |
| **LFU** | frequently used | stable hot sets where a one-off scan would otherwise evict everything |
| **FIFO** | newest | rarely the right answer, but O(1) and trivial |
| **TTL / volatile-\*** | unexpired | when everything is genuinely time-bounded |
| **W-TinyLFU** | frequency-sketched | what Caffeine uses; near-optimal hit rates at low overhead |

In Redis this is `maxmemory-policy`: `allkeys-lru` treats the instance as a cache,
`volatile-lru` only evicts keys that carry a TTL, and `noeviction` turns a full instance
into write errors. **Pick deliberately** — a cache that was silently running `noeviction`
is a classic 3 a.m. outage.

### The failure modes that only appear under load

This table is where the marks are.

| Failure | What happens | Fix |
|---|---|---|
| **Stampede / thundering herd** | a hot key expires and 10,000 concurrent requests all miss and all hit the database | **request coalescing** (single-flight: one loader, the rest wait), a short lock per key, `stale-while-revalidate` (serve the old value while one request refreshes), or probabilistic early expiry |
| **Penetration** | requests for keys that *don't exist* miss every time and reach the DB every time — the classic enumeration attack | **cache the negative result** with a short TTL, and/or a **Bloom filter** of existing keys in front |
| **Avalanche** | thousands of keys expire in the same second, because they were all populated at the same second (a deploy, a warm-up job) | **TTL jitter** — `ttl = base + rand(0, base/10)` |
| **Hot key** | one key's traffic exceeds a single cache node — no amount of sharding helps, because it's one key | **salt** it into N copies (`ad:42:{0..15}`, read a random one), or put an L1 in-process cache in front so each instance only fetches it once per second |
| **Big key** | one huge value (a 50 M-member ZSET) lives on one node, and any O(N) command blocks Redis's single thread for everyone | split by time window or range and merge in the app — see [Game Leaderboard](../hld/34-game-leaderboard.md) |

### Read-after-write consistency

A user who just wrote something and then doesn't see it will file a bug. Options, cheapest
first:

- **Update the cache on write** (write-through) — works when there's one writer path;
- **Read your own writes from the primary** for N seconds after a write, flagged in the
  session — the standard fix, and it also covers replica lag;
- **Version/monotonic token** — the client sends the version it last saw, and the read path
  refuses to serve older. Correct, and more machinery than most designs need.

Whatever you choose, say the line out loud: **the cache is never the source of truth**. If
the cache is lost entirely, the system must be slow, not wrong.

### Quick math

Effective latency is `h·c + (1−h)·d`. With a 1 ms cache and a 10 ms database:

| Hit ratio | Effective latency | DB load |
|---:|---:|---:|
| 0% | 10 ms | 100% |
| 80% | 2.8 ms | 20% |
| 95% | 1.45 ms | 5% |
| 99% | 1.09 ms | 1% |

The lesson is the right-hand column, not the middle one. Going from 80% to 99% barely moves
latency but takes the database from 20% of the load to 1% — **caching is a capacity
strategy that happens to also cut latency.**

---

## B) Sharding

### First: do you actually need to shard?

Sharding is the point where you give up joins, transactions and global uniqueness. It is
the last step, not the first. In order:

| # | Step | Buys | Cost |
|---:|---|---|---|
| 1 | **Index the query** | often 100× | none — do this first, every time |
| 2 | **Cache** | removes repeated reads | staleness |
| 3 | **Read replicas** | read throughput | replication lag, read-after-write problems |
| 4 | **Vertical scale** | everything, up to a point | money; modern instances reach hundreds of vCPUs and terabytes of RAM |
| 5 | **Table partitioning** (one node) | retention, pruning | partition key must be in the query |
| 6 | **Functional split** (tables → services) | isolates the hot workload | cross-service joins become API calls |
| 7 | **Horizontal shard** | unbounded writes and storage | everything in [what sharding breaks](#what-sharding-breaks) |

Steps 1–4 cover more businesses than people expect — see
[when you don't need distributed SQL](database-decision-list.md#when-you-dont-need-distributed-sql).
Sharding is forced by exactly three things: **write throughput** past one primary, **data
size** past one disk, or **geography** (latency or residency law).

### The six strategies

| Strategy | Key → shard | Wins | Loses | Seen in |
|---|---|---|---|---|
| **Range** | ordered key ranges | range scans stay local; trivially understandable | the newest range is a write hotspot for any time-ordered key | HBase, MongoDB ranged, [distributed SQL](database-decision-list.md#distributed-sql-cockroachdb-spanner-yugabytedb-tidb--the-pk-is-the-shard-map) |
| **Hash** | `hash(key) % N` | near-perfect spread | no range scans; **resizing moves nearly every key** | naive sharding, MongoDB hashed |
| **Consistent hashing** | position on a ring | resize moves only ~K/N keys | needs virtual nodes to be even | Cassandra, DynamoDB, memcached clients |
| **Directory / lookup** | an explicit shard map | arbitrary placement — move a whale tenant by editing a row | an extra hop, and the map is a SPOF you must cache and replicate | Vitess vindexes, per-tenant SaaS |
| **Geographic** | region column | latency, and data residency compliance | cross-region queries are the slow path | CockroachDB `REGIONAL BY ROW`, [Uber](../hld/23-uber.md) geosharding |
| **Functional / vertical** | by table or domain | simplest to reason about; usually the first split anyone does | doesn't help when *one* table is the problem | almost every system, early on |

Real systems combine them: **hash within a region, geographic across regions** is the
common multi-region shape.

### Consistent hashing in detail

The problem it solves: with `hash(key) % N`, going from 4 nodes to 5 remaps roughly **80%
of all keys** — every cache is cold and every lookup is wrong at once.

```
Hash nodes AND keys onto the same ring (0 … 2^32).
A key belongs to the first node clockwise from it.
Add a node → only the keys between it and its predecessor move.
```

- **Keys moved on a membership change: ~K/N** (K keys, N nodes) instead of ~K.
- **Virtual nodes are not optional.** With one point per node the ring is badly uneven —
  variance is huge at small N. Give each node 100–200 virtual points and the spread
  tightens; it also gives you **weighting** (a bigger machine gets more points) and spreads
  a failed node's load across *all* survivors rather than dumping it on one neighbour.
- **Redis Cluster uses a variant**: 16,384 fixed **slots**, `CRC16(key) mod 16384`, with
  slots explicitly *assigned* to nodes. The indirection means rebalancing is a deliberate
  slot migration rather than a consequence of the hash — which is easier to operate and
  reason about. Elasticsearch's fixed primary shard count is the same idea.
- **Where it shows up beyond databases:** websocket connection routing ([Google Docs](../hld/25-google-docs.md), [YouTube](../hld/17-youtube.md)),
  [rate-limiter shards](../hld/13-rate-limiter.md), and the
  [distributed cache](../hld/12-distributed-cache.md) problem, where the ring *is* the answer.

### Resharding without downtime

| Approach | How | Used by |
|---|---|---|
| **Fixed partitions ≫ nodes** | create e.g. 1024 partitions up front, assign many per node, move whole partitions to rebalance | Elasticsearch, Redis Cluster, Riak — **the best default** |
| **Dynamic splitting** | a partition splits when it exceeds a size threshold | HBase, CockroachDB ranges, MongoDB chunks |
| **Proportional to nodes** | fixed partitions *per node*; a new node steals a share from each existing one | Cassandra vnodes |
| **`hash % N`** | — | **never do this**; N can then never change |

The **migration playbook** when you must change the scheme anyway:

1. **Dual-write** to old and new, new is not read yet;
2. **Backfill** history into the new scheme;
3. **Verify** — shadow-read both and compare, with a mismatch metric, until it's flat;
4. **Flip reads** behind a flag, one percentage point at a time;
5. **Stop writing** the old one, and only then delete it.

Steps 3 and 4 are the ones people skip and the reason these migrations fail.

### Routing: who knows where the data is?

| Model | How | Trade |
|---|---|---|
| **Client-side** | the client library holds the topology and connects directly | no extra hop, lowest latency; every client must be upgraded when the topology model changes |
| **Routing tier / proxy** | a stateless proxy in the middle — Vitess, `mongos`, ProxySQL, Envoy | one place to change, connection pooling for free; one extra hop and one more tier to run |
| **Server-side redirect** | any node accepts and redirects — Redis `MOVED`/`ASK`, Cassandra's coordinator | dead simple clients; an extra round trip on a miss, unless the client caches the map |

Cassandra's leaderless design ([appendix](hld.md#2-cassandra)) means **any node can
coordinate** — that is the same idea taken to its conclusion, and it is why Cassandra has
no routing tier to run.

### What sharding breaks

| Broken | Why | What you do instead |
|---|---|---|
| **Joins** | the other side of the join is on another node | denormalise (the [Cassandra data model](hld.md#data-model-aka-query-driven)), or join in the application, or keep small dimension tables replicated everywhere (Citus reference tables) |
| **Transactions** | two shards means two commit paths | co-locate the rows so the transaction is single-shard (the real fix), or 2PC (slow, blocking), or a **saga** with compensating actions |
| **Global uniqueness** | `AUTO_INCREMENT` is per-shard | UUIDv7/ULID, Snowflake IDs (timestamp + shard + counter), or a dedicated ID service |
| **Global secondary indexes** | an index on a non-shard column is spread everywhere | *local* index + scatter-gather (fine at low shard counts), or a *global* index that is itself sharded on the indexed column and updated asynchronously |
| **Global ordering** | no shared clock, no shared log | per-shard ordering only, and design so that's enough — [WhatsApp](../hld/10-whatsapp.md) explicitly orders per chat and not across chats |
| **`COUNT(*)` and aggregates** | must touch every shard | maintain rollup counters, or push analytics into an [OLAP store](database-decision-list.md#olap-pinot-druid-clickhouse) |
| **Even load** | one tenant is 40% of your traffic | directory sharding so the whale gets its own shard, or salt its key — the **hot tenant** problem |

The choice of shard key is where all of this is decided; see
[key design per store](database-decision-list.md#key-design-per-store) for how each store
spells it and what it punishes.

---

## C) Load balancing

### The layers

| Layer | Tech | Decides on | Strength | Limit |
|---|---|---|---|---|
| **DNS / GSLB** | Route 53, GeoDNS, anycast | client geography, coarse health | routes users to the nearest region for free | resolver TTL caching means failover takes minutes, and clients ignore your TTLs |
| **L4 (transport)** | NLB, IPVS, Maglev | IP + port | millions of connections, microsecond overhead, protocol-agnostic | can't see paths, headers or HTTP status; can't retry |
| **L7 (application)** | ALB, nginx, HAProxy, Envoy | path, header, cookie, method | routing rules, retries, TLS termination, rate limits, canaries | more CPU per request; a hop that can itself be saturated |
| **Client-side / mesh** | gRPC + xDS, Envoy sidecar | full server list per client | no extra network hop; per-request balancing on long-lived connections | every client needs the topology, and a control plane to feed it |

The subtlety worth stating: **L4 balances connections, L7 balances requests.** With HTTP/2
or gRPC, one connection carries thousands of requests, so an L4 balancer pins all of them
to whichever backend it picked first. That is why gRPC services use client-side balancing
or an L7 proxy — an L4 balancer in front of gRPC quietly produces badly skewed load.

### The algorithms

| Algorithm | How | Good when | Fails when |
|---|---|---|---|
| **Round robin** | next in the list | requests are uniform and backends identical | one slow request type piles onto whoever gets it |
| **Weighted RR** | proportional to declared capacity | heterogeneous instance sizes | weights are static and reality isn't |
| **Least connections** | fewest in-flight | request costs vary a lot — **the best simple default** | needs shared state, so it's per-balancer |
| **Least response time / peak-EWMA** | latency-weighted moving average | backends degrade gradually rather than failing | can oscillate; needs damping |
| **Power of two choices** | pick 2 at random, send to the less loaded of the two | **near-least-connections quality with O(1) state and no coordination** — the one to name in an interview | nothing much; this is the modern default |
| **Consistent hash / IP hash** | hash the key to a backend | cache locality, sticky routing to a shard owner | uneven when keys are skewed |
| **Maglev hashing** | consistent hashing tuned for connection tables | L4 balancers needing minimal disruption on backend change | fixed-size table needs sizing |
| **Random** | uniform pick | genuinely uniform workloads | tail latency is worse than P2C for free |

**Power of two choices** is the answer to "how do you balance without a coordinator?" and it
is worth knowing why it works: random assignment gives a maximum load of ~log n/log log n,
while picking the better of two random options drops that to ~log log n — an exponential
improvement for one extra probe.

### Session affinity and the websocket problem

Stickiness — cookie-based, or consistent hashing on a user ID — is a workaround, not a
design. **The real answer is to make the service stateless and put the state in Redis**,
which is what [rate limiting](../hld/13-rate-limiter.md) and session storage do.

You genuinely can't avoid affinity when the connection *is* the state:

- **Long-lived connections don't rebalance.** Add three instances to a websocket tier and
  they receive nothing, because every client is already connected elsewhere. New capacity
  only helps new connections.
- **A deploy is a mass reconnect.** Every dropped client reconnects at once — a
  self-inflicted thundering herd. Mitigate with **connection draining** (stop accepting,
  let existing connections finish), **jittered client backoff**, rolling restarts small
  enough that the surviving instances have headroom, and enough spare capacity to absorb
  the reconnect spike.
- **Finding a user's connection** becomes a routing problem of its own: a registry mapping
  `user_id → server` in Redis, or consistent hashing so any node can compute the owner.
  [Google Docs](../hld/25-google-docs.md), [WhatsApp](../hld/10-whatsapp.md) and
  [FB Live Comments](../hld/19-fb-live-comments.md) all land here, and all reach for
  [Redis pub/sub](hld.md#pubsub) to fan a message out to whichever node holds the socket.

### Health checks, and how they cause outages

| | Shallow | Deep |
|---|---|---|
| Checks | process alive, port open | can reach the database, dependencies healthy |
| Catches | crashes, hangs | genuine inability to serve |
| **Risk** | serves errors happily | **a database blip fails every instance at once, the balancer removes all of them, and a degradation becomes a total outage** |

The resolution is standard and worth stating precisely:

- **Separate liveness from readiness.** Liveness ("am I wedged? restart me") must be
  shallow. Readiness ("should I get traffic?") can be deeper.
- **Fail static.** If *every* backend is unhealthy, a good balancer sends traffic anyway —
  degraded service beats no service. AWS calls it fail-open; Envoy calls it panic mode.
- **Passive checks / outlier detection** eject a backend that is *actually returning
  errors*, and cap how much of the fleet can be ejected at once (Envoy defaults to 10%).
- **Active checks** must be cheap and jittered, or the health checks themselves become a
  meaningful fraction of your traffic.

### Overload protection

Balancing decides *where* work goes. These decide **whether it goes at all**, and they
matter more.

| Mechanism | What it does | Getting it wrong |
|---|---|---|
| **Timeouts** | bounds how long one request can hold a thread | no timeout means one slow dependency consumes the whole thread pool |
| **Retries + backoff + jitter** | recovers from transient failures | naive retries **multiply** load exactly when the system is struggling — three tiers each retrying 3× is 27× amplification |
| **Retry budget** | caps retries at e.g. 10% of requests | without it, the retry storm is indistinguishable from a DDoS you aimed at yourself |
| **Circuit breaker** | stops calling a dependency that is failing, probes occasionally | tripping too eagerly turns a blip into an outage |
| **Concurrency limits** | bounded in-flight requests per instance, queue the rest, shed past that | better than a rate limit — it adapts to how expensive requests actually are |
| **Load shedding** | reject low-priority work first, cheaply, at the edge | shedding *after* the expensive work is done buys nothing |
| **Backpressure** | the queue depth signals producers to slow down | an unbounded queue converts an overload into an OOM plus unbounded latency |

Two design-level versions of the same idea in this set: the
[Notification System](../hld/33-notification-system.md)'s priority partitions let it drop
low-priority traffic without starving it, and the [Rate Limiter](../hld/13-rate-limiter.md)
is admission control as a service.

### Multi-region traffic

| Shape | Failover | Cost | Use when |
|---|---|---|---|
| **Active-passive** | DNS/health flip, minutes; the standby is unexercised and often broken when needed | idle capacity | DR requirement, not a latency one |
| **Active-active, regional data** | seconds — traffic just goes elsewhere | needs a partitionable data model | users belong to a home region — the [gaming/residency](database-decision-list.md#online-gaming--betting--cockroachdb-with-geo-partitioning-plus-redis) shape |
| **Active-active, global consistency** | seconds | cross-region consensus latency on every write | money that must be globally serializable — [payments](database-decision-list.md#core-payment-platforms--cockroachdb-or-spanner) |

Two containment patterns worth naming:

- **Cell-based architecture** — run many independent full-stack cells, each serving a slice
  of users. A bad deploy or a poison-pill request takes out one cell, not the service.
- **Shuffle sharding** — assign each tenant a *random pair* of cells out of N. With 8 cells
  and pairs, two tenants share both cells only rarely, so one abusive tenant degrades a
  small, computable fraction of the others rather than everyone.

### Autoscaling

- **Scale on the signal that reflects the bottleneck** — queue depth or in-flight
  concurrency, not CPU. A thread-pool-bound service is 100% saturated at 30% CPU.
- **Scale-up is a feedback loop with the database.** More app instances means more
  connections means a slower database means slower requests means the autoscaler adds more
  instances. Put a connection pooler (PgBouncer) in front and cap the pool, or the
  autoscaler will happily scale you into an outage.
- **Warm-up matters** — JIT, connection pools and local caches mean a fresh instance is
  slower than a warm one, so scaling *at* saturation is already too late. Scale on a
  leading indicator and keep headroom.
- **Scale-in cautiously**: aggressive scale-in plus long-lived connections means repeated
  mass reconnects.

---
## D) Scaling triggers — when one machine is actually done

"Add a server" is only a defensible answer if you can say *at what number*. This section
pins a concrete reference machine and gives the thresholds against it.

### The reference machine

Everything below assumes one **8 vCPU / 32 GB / NVMe-backed / up to 10 Gbps** node —
`m7i.2xlarge`, `m8g.2xlarge`, `c4-standard-8`, or the equivalent 8-core VM on any provider.
That is the everyday production instance in 2026: big enough to be realistic, small enough
that the numbers are memorable. **Scale every threshold roughly linearly with core count**;
memory-bound ones scale with RAM instead.

For context on the headroom above it, the largest single instances currently available:

| | Reference box | Largest generally available |
|---|---|---|
| vCPU | 8 | ~192 (`m8g.48xlarge`, Graviton4) |
| RAM | 32 GB | 1.5 TB (`r8g.48xlarge`), up to 24–32 TB on high-memory `u7i` |
| Local NVMe | ~1–2 GB/s | 10+ GB/s, millions of IOPS (`i8g`) |
| Network | up to 10 Gbps | 100–200 Gbps |
| EBS/network disk | gp3: 16k IOPS, 1 GB/s | io2 Block Express: 256k IOPS, 4 GB/s |

That is roughly **24× more CPU and 50–1000× more RAM** than the reference box. Which is the
point of [step 4 on the ladder](#e-the-escalation-ladder): most systems that "need to shard"
have simply never been vertically scaled.

### What the reference box actually serves

Order-of-magnitude anchors for a *tuned* single node. Measure your own — quote these only
to justify the shape of an answer, never as a promise.

| Workload | Reference box (8 vCPU / 32 GB) | Bound by |
|---|---|---|
| **Stateless JSON API** (Go / JVM, trivial handler) | 8–15k RPS | CPU, then GC |
| **API doing one DB call + serialisation** | 1.5–4k RPS | the DB, and the pool |
| **nginx / Envoy proxying** | 50–100k RPS | CPU + syscalls |
| **PostgreSQL, cached reads** | 10–25k TPS | CPU |
| **PostgreSQL, writes (`fsync` on NVMe, group commit)** | 3–8k TPS | WAL fsync, then checkpoints |
| **PostgreSQL, comfortable data size** | ~0.5–1 TB, while the **hot set fits in ~24 GB** | page cache |
| **Redis** | ~100k ops/s, ~1M/s pipelined | **one core** — Redis is single-threaded |
| **Redis, usable memory** | ~20–24 GB with persistence, ~28 GB without | fork copy-on-write headroom |
| **Kafka broker** | 100–300 MB/s in | disk, then network |
| **Elasticsearch** | 16 GB heap (never above 31 GB), ~300 shards | heap, merges |
| **Websocket fan-out** | 50–200k idle connections | memory per connection, FDs |

The two most important lines: **Redis uses one of your eight cores**, so a "CPU 15% busy"
Redis box can be completely saturated; and a Postgres node is fine at a terabyte *as long as
the working set fits in RAM* — the cliff is the hot set, not the disk.

### The rule behind every threshold: the utilisation knee

Queueing theory sets the trigger, not taste. For a single server at utilisation ρ, waiting
time scales as `1 / (1 − ρ)`:

| Utilisation | Latency multiplier | On the reference box |
|---:|---:|---|
| 50% | 2× | 4 cores busy — comfortable |
| 70% | 3.3× | 5.6 cores busy — **act here** |
| 80% | 5× | 6.4 cores busy — p99 is already visibly degraded |
| 90% | 10× | 7.2 cores busy — one traffic bump from an incident |
| 95% | 20× | 7.6 cores busy — effectively down |

That curve is why every threshold below sits at **60–75%, not 90%**. Real workloads are
burstier than the M/M/1 model, so the knee arrives *earlier* than the table says, and the
gap between "fine" and "on fire" is one busy minute wide.

The companion rule is **Little's Law — `concurrency = throughput × latency`**. It sizes
every pool and explains every collapse: at 2,000 RPS and 50 ms, you need 100 in-flight
slots; if latency degrades to 200 ms, the same traffic demands 400 and your 100-thread pool
starts queueing — which raises latency further. That feedback loop is what turns a 3× slow
dependency into an outage.

### Which signal to scale on

| Signal | Leading? | Good for | Trap |
|---|---|---|---|
| **In-flight concurrency** | ✅ | the best general-purpose signal — it's Little's Law, directly measured | needs instrumentation, not just node metrics |
| **Queue depth / consumer lag** | ✅ | anything async | absolute lag is noise; scale on the **derivative** — is it growing? |
| **Pool wait time / rejections** | ✅ | thread pools, connection pools | any value above zero is already the trigger |
| **CPU utilisation** | ⚠️ lagging | CPU-bound services only | useless for IO-bound, thread-bound, or **single-threaded** (Redis, Node) workloads |
| **p99 latency** | ❌ lagging | user-visible SLO alarms | by the time it moves, users already felt it |
| **RPS** | ⚠️ | uniform request cost | one expensive endpoint invalidates it |

Name the frameworks if asked: **USE** (Utilisation, Saturation, Errors) for resources,
**RED** (Rate, Errors, Duration) for services, and Google's four golden signals (latency,
traffic, errors, saturation). Saturation is the one people omit and the one that predicts.

### The concurrency trigger, concretely

Concurrency is the best signal because it is the thing that actually contends. RPS conflates
a 2 ms cache hit with a 400 ms report; **in-flight requests** is the number that fills thread
pools, connection pools and CPU run queues alike. Little's Law converts between them
whenever you need to: `concurrency = RPS × latency`.

Every tier has an **N\*** — the in-flight count at which throughput stops improving. Past
N\*, throughput doesn't plateau, it *falls*: the Universal Scalability Law's coherency term
means added concurrency spends more time on contention than on work. So the concurrency
limit is a real ceiling, and the scale trigger sits below it.

#### Compute: N\* ≈ cores × (1 + wait/compute)

The sizing formula (Goetz) with a 75% utilisation target on the reference box's 8 cores:

```
N* = cores × U_target × (1 + W/C)      →      N* = 8 × 0.75 × (1 + W/C)
```

| Workload | W/C | N\* on 8 vCPU | Scale-out trigger (70% of N\*) | At 50 ms p50, that's |
|---|---:|---:|---:|---:|
| **Pure CPU** (encode, hash, render) | 0 | **6** | 4 in flight | ~90 RPS |
| **Typical API** — 10 ms CPU, 40 ms DB wait | 4 | **30** | 21 in flight | ~420 RPS |
| **IO-heavy** — 5 ms CPU, 95 ms of calls | 19 | **120** | 84 in flight | ~840 RPS |
| **Async / event-loop** (Go, Node, Netty) | — | 100s–1000s | memory + downstream, not threads | — |

So the headline number for the reference box running an ordinary service: **N\* ≈ 30–50
in-flight requests, trigger scale-out at a sustained ~25–35.** Everything else is a
consequence of that — the "~700 RPS" figure people quote is just `35 ÷ 50 ms`, and it moves
the moment latency does.

Three practical notes:

- **Measure N\*, don't compute it.** Ramp offered concurrency and watch throughput; the point
  where it flattens is N\*, and the point where it *declines* is where a static limit saves
  you. The formula is for sizing a first guess.
- **Cap it, then shed.** A bounded concurrency limit with a small queue and fast rejection
  beats an unbounded pool every time — it converts an overload into a few fast 503s instead
  of a fleet-wide latency collapse.
- **Adaptive limits are the modern answer**: Envoy's adaptive concurrency filter and
  Netflix's `concurrency-limits` (Gradient2) sample minimum RTT and shrink the limit as
  latency inflates — TCP Vegas applied to RPCs. They find N\* continuously instead of making
  you re-tune it after every deploy.

#### Database: N\* ≈ 2–4 × cores, and it is a *global* budget

A database's concurrency ceiling is far lower than people expect, and it is the same shape
everywhere: throughput peaks at a small multiple of cores and degrades past it, because
every extra backend adds lock contention, context switches and snapshot work.

| Store | N\* on 8 cores | Signal that you're past it | Knob |
|---|---:|---|---|
| **PostgreSQL** | **16–24 active** — the classic `(2 × cores) + spindles` | pool wait > 0; throughput falls as connections rise | `default_pool_size` in PgBouncer — **not** `max_connections` |
| **MySQL / InnoDB** | 16–32 active | rising `Threads_running` with flat QPS | `innodb_thread_concurrency`, or a pooler |
| **MongoDB** | 128 read + 128 write tickets (dynamically tuned in recent versions) | `queued.readers` / `queued.writers` > 0 — **the** MongoDB concurrency trigger | ticket limits; usually a sign to fix the query |
| **Cassandra** | `concurrent_writes ≈ 8 × cores` = 64, `concurrent_reads ≈ 32` | pending tasks in the mutation/read thread pools | those two settings, plus disk |
| **Redis** | **1** | it's single-threaded — concurrency isn't the trigger, ops/s on one core is | pipelining, then sharding |

For the reference box, PostgreSQL is the one that bites: **~20 active connections is the
peak, and 200 is slower than 20.** Two things follow.

**First: never raise `max_connections` as a fix.** It moves you further past the knee, and
each backend costs 5–10 MB before it does any work.

**Second, and this is the one that actually breaks systems: the database's N\* is a budget
shared by the whole fleet, not a per-instance setting.**

```
20 app instances × pool of 20 = 400 connections
     → into a Postgres that peaks at ~20 active
     → adding app instances makes the database SLOWER
```

That is the [feedback loop](#autoscaling) stated numerically, and it is why "we scaled out
and latency got worse" is such a common story. The fix is not arithmetic (a pool of 1 per
instance is unworkable — it serialises each instance); it is **a pooler in the middle**:
400 client connections terminate at PgBouncer in transaction mode, which multiplexes them
onto ~20 server connections. The app tier then scales freely, and the database never leaves
its peak.

#### The chain, sized end to end

The constraint that has to hold across the whole path:

```
app concurrency limit  ≥  its DB pool size
Σ (DB pool size across every instance)  ≤  database N*
```

Worked for the reference box, 10 app instances in front of one 8-core Postgres:

| Tier | Setting | Number |
|---|---|---:|
| Database N\* | peak active connections | **20** |
| PgBouncer | `default_pool_size` (server side) | 20 |
| PgBouncer | client connections accepted | 1,000+ |
| Per app instance | DB pool (to PgBouncer) | 20 |
| Per app instance | concurrency limit | 30 |
| **Fleet** | 10 × 30 = 300 in flight, all funnelling to 20 server connections | — |
| **Scale-out trigger** | sustained in-flight > 21/instance (70% of 30) | — |
| **Stop-scaling signal** | PgBouncer `cl_waiting` > 0 sustained | — |

The last row is the important one. Once clients are queueing at the pooler, **more app
instances add zero throughput** — the trigger has stopped meaning "scale out" and started
meaning "the database is the bottleneck", which sends you to
[read replicas, caching, or sharding](#e-the-escalation-ladder) instead.


### TPS ceilings, and what pooling actually buys

**Pooling does not raise the ceiling. It stops you falling off it.** The ceiling is set by
Little's Law run backwards — at peak concurrency N\*, throughput is bounded by how long each
transaction holds a slot:

```
TPS = N* / service time      →      on the reference box:  TPS = 20 / t
```

That is the whole model. Everything else is a fight over `t`.

#### What 8 vCPU of PostgreSQL actually does

Reference box, NVMe, hot set in RAM, N\* = 20 active connections:

| Transaction | Service time `t` | Ceiling `20/t` | Realistic sustained |
|---|---:|---:|---:|
| `pgbench -S`, prepared, single row, local socket | 0.1 ms | 200k | **100–150k** — the synthetic number; don't quote it as capacity |
| Indexed point read, few columns, over the network | 0.3–0.5 ms | 40–65k | **25–40k** |
| Read via an app (RTT + parse + pool checkout) | 0.5–1 ms | 20–40k | **10–25k** |
| Read with a join returning ~100 rows | 2–5 ms | 4–10k | **4–8k** |
| Single-row `INSERT`/`UPDATE`, group commit on NVMe | 0.5–2 ms | 10–40k | **3–8k** |
| Write with 3 indexes + FK checks | 3–8 ms | 2.5–6k | **1–3k** |
| Multi-statement transaction (`BEGIN` + 5 queries + `COMMIT`) | 5–15 ms | 1.5–4k | **1–3k** |

The [earlier capacity table](#what-the-reference-box-actually-serves) quotes 10–25k cached
reads and 3–8k writes — those are the two "realistic sustained" rows, and they're the ones
to use in a design.

Two things fall out that matter more than the numbers:

- **Round trips dominate short transactions.** Five statements at 0.2 ms network RTT is 1 ms
  of pure waiting — often more than the queries cost. Batching five statements into one
  round trip can double TPS without touching the database.
- **A slot held idle is a slot removed from N\*.** With N\* = 20, an endpoint that keeps a
  transaction open for 200 ms across an external API call consumes 5% of your *entire*
  database capacity per concurrent request. `idle_in_transaction_session_timeout` exists for
  this reason. Never hold a transaction across a network call you don't control.

#### What pooling changes

| Setup | Effective TPS on reads | Why |
|---|---:|---|
| **No pool** — connect per request | **0.5–2k** | a new backend is a process fork plus TLS: 1–5 ms and real CPU, before any query runs |
| **App-side pools only** — 20 instances × 20 = 400 backends | **8–15k** | you're 20× past N\*: lock contention, context switching and snapshot work eat 40–60% of peak |
| **PgBouncer, transaction mode** — 400 clients → 20 server connections | **20–40k** | the database runs at exactly N\*; everything else queues politely outside it |

So pooling is worth roughly **10–30× against no pooling at all**, and **~2–3× against an
over-connected fleet** — but it never beats the `20/t` ceiling. If you need more than that,
`t` has to come down or the work has to leave the primary.

**Pool mode decides the multiplexing ratio**, which is the entire point:

| Mode | Server connection released | Client:server ratio | Cost |
|---|---|---:|---|
| **Session** | at disconnect | ~1:1 | barely better than connecting directly |
| **Transaction** | at `COMMIT` | **20:1 to 100:1** | no session state — `SET`, advisory locks, temp tables, `LISTEN`/`NOTIFY` all break |
| **Statement** | per statement | highest | multi-statement transactions are impossible |

**Transaction mode is the one you want**, and its historical objection is gone: PgBouncer
1.21+ supports protocol-level prepared statements in transaction mode, so ORMs and JDBC no
longer have to disable server-side prepares.

#### The pooler is the next bottleneck

**PgBouncer is single-threaded** — the same trap as [Redis](#database-triggers-on-the-same-box).
One process tops out around **10–30k TPS** regardless of how much CPU the box has, which
lands right on top of the ceiling it was protecting. When you get there:

- run several PgBouncer processes behind `SO_REUSEPORT` (`so_reuseport=1`), one per core;
- or move to a multi-threaded pooler — **Odyssey** or **PgCat**;
- or use the provider's managed pooler (RDS Proxy, Cloud SQL's built-in), which handles this.

#### Getting past the ceiling

In the order you should try them:

| Lever | Effect on the reference box | Cost |
|---|---|---|
| **Batch statements** — fewer round trips per transaction | often 2× — pure win | code change |
| **Shorten transactions** — no external calls inside, commit early | proportional: halve `t`, double TPS | discipline |
| **Cache the reads** | at 95% hit rate, the DB sees 5% of the load — a **20× multiplier** | staleness |
| **`synchronous_commit = off`** for non-critical writes | 5–10× write TPS | a crash loses the last ~200 ms of commits (no corruption) |
| **Read replicas** | reads scale ~linearly per replica; writes do not move at all | replication lag |
| **Bigger box** | ~linear in cores — 32 vCPU gives N\* ≈ 80, so ~4× | money |
| **Shard** | unbounded | [everything sharding breaks](#what-sharding-breaks) |

The write path is the one that eventually forces the decision: replicas don't help it,
caching doesn't help it, and a single primary's fsync path stops at **~5k sustained write
TPS** on this box. That number is the shard trigger.

#### Sizing an application against it

Work backwards from DB time per request, not from RPS:

```
DB time per request  =  Σ (queries × service time)
max app RPS          =  N* / DB time per request
```

A request doing 3 point reads (0.5 ms) and 1 write (1 ms) spends **2.5 ms** in the database,
so one 8 vCPU Postgres supports `20 / 2.5 ms` = **~8,000 requests/sec** — comfortably more
than the [10 app instances sized in the chain above](#the-chain-sized-end-to-end) can
generate. If the same request instead does 15 queries (a classic N+1), DB time is ~8 ms and
the ceiling drops to **2,500 RPS** — a 3× capacity loss caused entirely by query count, and
fixable without a single new machine.

That is the honest summary of this whole section: **the pool protects the ceiling, the query
count sets it.**


### Per-resource triggers on the reference box

| Resource | Watch | Trigger | Why there | First move |
|---|---|---|---|---|
| **CPU** | utilisation, run-queue length | **> 70% sustained 5 min**, or run queue > 8 (1/core) | the knee above | scale out — but confirm the app tier is the bottleneck first |
| **CPU throttling** (containers) | cgroup `throttled_time` ratio | **> 1% of periods throttled** | a pod with a 2-core limit throttles hard while the *node* reads 30% busy | raise the CPU limit, or remove it and rely on requests |
| **Memory** | RSS vs limit, OOMKills | **> 75% (24 GB)**, or any OOMKill | leaves room for page cache and allocation spikes | size up before out — memory is the cheapest axis |
| **JVM heap** | occupancy *after* full GC, GC time | **> 70% after GC**, or GC > 5% of wall clock | post-GC occupancy is live-set size; the rest is noise | tune, then size up |
| **Disk IOPS** | % of provisioned, `await` p99 | **> 70% of provisioned**, or await p99 > 10 ms | gp3 gives 16k IOPS — you hit the *provisioned* ceiling long before the device's | provision more IOPS (a one-line change) before anything structural |
| **Disk queue** | `avgqu-sz` | consistently > 2× the device's parallelism | `%util` is **meaningless on NVMe** — it saturates at 100% while the device is bored | — |
| **Network** | Gbps, packets/sec, ENA allowance counters | **> 60% of the 10 Gbps allowance**, or any `bw_*_allowance_exceeded` / `pps_allowance_exceeded` | cloud NICs shape by *credits*: the box throttles silently, with no CPU or error signal | this is the most-missed trigger of the three |
| **Connections** | conntrack table, ephemeral ports, `TIME_WAIT` | **> 70% of the table**, or ports exhausted | ~28k ephemeral ports per destination pair caps outbound fan-out | connection reuse / keep-alive before more machines |
| **File descriptors** | open FDs vs `ulimit` | **> 70%** | 200k websockets needs the limit raised, and about 8–16 GB | raise the limit; then it's a memory question |
| **Thread / worker pool** | queue wait time, rejections | **any rejection**, or wait p99 > 10 ms | Little's Law — the pool is already the constraint | bound the queue, shed load, *then* scale |
| **Connection pool** | wait time | **wait p99 > 0** | you are queueing on the database, not on the app | a pooler (PgBouncer), not more app instances |

### Database triggers on the same box

**PostgreSQL** — the order in which you'll actually hit them:

| Signal | Trigger | Response |
|---|---|---|
| Active connections | **> ~32 active** (4 × cores); pool wait > 0 | PgBouncer in transaction mode — adding app nodes here makes it *worse* |
| Buffer cache hit ratio | **< 99%** for OLTP | the hot set no longer fits 24 GB → more RAM, or partition/archive cold data |
| Replication lag | **> your read-after-write SLA** (often 1 s) | route reads that need freshness to the primary |
| Dead tuples | **`n_dead_tup` / `n_live_tup` > 20%**, or growing monotonically | autovacuum can't keep up — tune it before it becomes bloat you can only fix with a rewrite |
| Transaction ID age | **> 200M** | wraparound risk — this is a wake-someone-up trigger, not a scaling one |
| Checkpoints | `checkpoints_req` ≫ `checkpoints_timed` | raise `max_wal_size`; you're checkpointing on volume, which stalls writes |
| Write TPS | **> ~5k sustained** on one primary | the genuine shard signal — a single primary's fsync path is the wall |
| Table size | **> ~100 GB**, or an index that no longer fits RAM | partition (step 5), don't shard (step 9) |

**Redis** — remembering it gets one core:

- **CPU of the Redis process > 70% of one core** — the real saturation signal; node-level CPU will read ~10% and tell you nothing.
- `evicted_keys` > 0 when you didn't intend eviction, or `used_memory` **> 24 GB** — persistence forks need copy-on-write headroom, so plan for ~70% of RAM, not 95%.
- `blocked_clients` rising, or `slowlog` entries — almost always one O(N) command against a big key.
- Latency spikes exactly at `BGSAVE` — a fork stall, fixed by more headroom or by moving persistence to a replica.

**Kafka** — consumer **lag derivative** > 0 sustained (absolute lag is meaningless during a
burst); consumers = partitions, at which point only a partition increase adds parallelism;
ISR shrinking; request-handler idle ratio < 30%.

**Elasticsearch** — heap > 75% after GC; more than ~20 shards per GB of heap (so ~320 shards
on a 16 GB heap); search queue rejections > 0.

### Cloud-era triggers people miss

1. **HPA on CPU is the wrong default.** It's what every tutorial uses and it's a lagging
   signal for the IO-bound services most people run. Scale on concurrency or queue depth
   via custom/external metrics or **KEDA** instead.
2. **CFS throttling is the most common invisible bottleneck now.** A container with
   `limits.cpu: 2` is throttled every 100 ms period once it uses 200 ms of CPU — latency
   goes to pieces while every dashboard shows a bored node. Check `throttled_time` before
   you believe any CPU graph in Kubernetes.
3. **Pending pods are a cluster-level trigger.** The service scaled; the cluster didn't.
   Cluster Autoscaler / Karpenter provisioning time is now part of your ramp.
4. **Scale-to-zero has a cold-start tax** — the trigger must fire early enough to cover it.

### Trigger lead time — the formula that sets the threshold

You cannot trigger at the threshold you want to *hold*; you must trigger far enough ahead to
cover the ramp:

```
ramp = detection interval + autoscaler cooldown + provisioning + warm-up
required headroom = peak growth rate × ramp
```

Worked on the reference box: metrics every 60 s, a 60 s cooldown, ~90 s to boot and pass
health checks, ~60 s to warm JIT, pools and local caches → **ramp ≈ 4.5 min**. Traffic that
can double in 10 minutes grows ~10%/min, so you need **~45% headroom** — which lands the
trigger at about **55–60% CPU**, not 70%. Shorten the ramp (pre-baked images, pre-warmed
pools, warm standby capacity) and you can run hotter; that trade is the whole of capacity
planning.

### Anti-triggers

| Trap | What happens |
|---|---|
| Scaling the app tier when the **database** is saturated | more instances open more connections, and the database gets slower — the [feedback loop](#autoscaling) |
| Averaging CPU **across the fleet** | one hot shard at 95% hides inside a fleet average of 40% — alert on the max, not the mean |
| Scaling on **p50** | p50 is flat while p99 has already tripled; users live in the tail |
| A metric with a **collection interval longer than the spike** | a 5-minute average never sees a 90-second overload that dropped 20% of requests |
| **No scale-in guardrail** | scale-out and scale-in oscillate; use a stabilisation window, and be far more conservative shrinking than growing |
| Triggering on a metric the **new instances change** | new nodes cold-start, latency rises, the autoscaler adds more — a runaway |

### Trigger → cheapest correct response

| Trigger | First move (not "add a server") |
|---|---|
| CPU > 70% on the app tier | scale out — this is the one case where it *is* the answer |
| Connection pool wait > 0 | PgBouncer / pool tuning |
| Postgres cache hit < 99% | more RAM, or archive cold rows |
| Postgres write TPS at the wall | batch, async the non-critical writes, then shard |
| Redis at 70% of one core | shard by key, or add a read replica; more cores do nothing |
| Consumer lag growing | more partitions *and* more consumers — partitions alone don't help |
| Network allowance exceeded | compress, cache at the edge, move bytes to a CDN or S3 |
| p99 up, CPU flat | look for a lock, a pool, GC, or CFS throttling — this is never a capacity problem |


---

## E) The escalation ladder

When asked "how would you scale this?", walk it in this order. Each step is cheaper and
less destructive than the next, and stopping early is a *correct* answer, not a lazy one.

| # | Step | Buys | Gives up |
|---:|---|---|---|
| 1 | Index the query, fix the N+1 | orders of magnitude | nothing |
| 2 | CDN / edge for static and media | most of the bytes | nothing you cared about |
| 3 | Cache the hot reads | database load | freshness |
| 4 | Read replicas | read throughput | read-after-write consistency |
| 5 | Async the non-critical path (queue it) | tail latency, spike absorption | immediate feedback to the user |
| 6 | Vertical scale | everything, once | money |
| 7 | Precompute a read model | expensive reads become lookups | a pipeline to maintain and backfill |
| 8 | Functional split | isolation of the hot part | cross-service joins |
| 9 | Shard | writes and storage without bound | joins, transactions, ordering, uniqueness |
| 10 | Multi-region | latency and residency | consensus latency, or consistency |

---

## F) Problem-set map

The one decision in each design that these three levers actually settled:

| Problem | Caching | Sharding | Load balancing / traffic |
|---|---|---|---|
| [Bit.ly](../hld/01-bitly-url-shortener.md) | cache-aside on `short_code`; the redirect is the *only* hot path | not needed — 1 TB fits one primary | stateless app tier, plain round robin |
| [Dropbox](../hld/02-dropbox.md) | CDN for downloads; metadata cached per user | metadata by `user_id`; bytes in S3 | presigned URLs take the bytes off your fleet entirely |
| [TicketMaster](../hld/05-ticketmaster.md) | cache the event/seat map aggressively — it barely changes | by `event_id`; one event is one shard's problem | queue-based admission at the edge for on-sale spikes |
| [Instagram](../hld/06-instagram.md) | the feed *is* a cache — a precomputed Redis list | posts by `user_id` | hybrid fan-out: push for normal users, pull for celebrities, because one hot key can't be sharded |
| [WhatsApp](../hld/10-whatsapp.md) | none on the message path — it's write-heavy | outbox by `user_id` | websocket registry + [pub/sub](hld.md#pubsub) to reach the node holding a socket |
| [Distributed Cache](../hld/12-distributed-cache.md) | *is* the cache | consistent hashing ring + vnodes | client-side routing, no proxy |
| [Rate Limiter](../hld/13-rate-limiter.md) | Redis holds all state; TTL is the eviction | by API key | it *is* admission control for everyone else |
| [YouTube](../hld/17-youtube.md) | CDN is ~99% of the design | metadata sharded; chunks in S3 | GeoDNS to the nearest edge |
| [Uber](../hld/23-uber.md) | **write-back** cache for the location firehose | geosharding — the shard key is geography | matching is a spatial query, not a balancing problem |
| [Ad Click Aggregator](../hld/27-ad-click-aggregator.md) | pre-aggregated OLAP tables are the cache | Kafka partitions **salted** on `ad_id` | back-pressure via Kafka; consumers scale to partition count |
| [Google Docs](../hld/25-google-docs.md) | the live document lives in memory | consistent hashing on `doc_id` → one owner per doc | sticky websockets; a deploy needs draining |
| [Notification System](../hld/33-notification-system.md) | — | Kafka partition per priority | priority-aware load shedding: drop low priority, never starve it |
| [Game Leaderboard](../hld/34-game-leaderboard.md) | Redis ZSET is the serving layer | shard by window/score band to avoid one big key | read replicas for the top-K read fan-out |

---

## Related

- [Database decision list](database-decision-list.md) — which store, and
  [key design per store](database-decision-list.md#key-design-per-store) for the shard key itself
- [Isolation levels](isolation-levels.md) — what you can still promise once it's distributed
- [HLD appendix](hld.md) — [partitioning](hld.md#5-partitioning-sharding),
  [leaderless replication](hld.md#6-sharding--replication-leaderless),
  [Redis](hld.md#4-redis), [Kafka](hld.md#3-kafka)
- [HLD interview framework](../hld/00-interview-framework.md) — where in the flow this pass happens
