# Requirements ramp-up log

[← HLD index](README.md) · [Requirements index](requirements-index.md) · [Interview framework](00-interview-framework.md)

---

Seven gaps surfaced by three requirements rounds on the data-engineering anchors
([27 Ad Click](requirements-index.md#27-ad-click-aggregator),
[22 Top K](requirements-index.md#22-youtube-top-k),
[30 Metrics Monitoring](requirements-index.md#30-metrics-monitoring)). One sitting per gap,
3–5 drill items each, drawn from the whole list. A gap is **closed** when the pass criterion
holds in its own sitting *and* in a later mixed review.

## Gap status

| Gap | Skill | Pass criterion | Status | Last sitting | Pass rate |
|---|---|---|---|---|---|
| G1 | **Find the tension** — the two requirements that fight; the crux | Conflicting pair + one-line resolution, unprompted, 4/5 | open | Sitting 7 | form ✓, pair ✗ |
| G2 | **CAP per operation** — "X is AP tolerating N; Y is CP because Z" | Per-op sentence with a number/reason, 4/5 | closing | Sitting 7 | structure ✓, vocab ✗ |
| G3 | **The "never" NFR** — lossless / idempotent / exact / durable / late data | Stated with *never / exactly / must not* before any scale NFR, 4/5 | open — regressed | Sitting 7 | ❌ in mixed |
| G4 | **Ask once, then commit** — no "tolerable? assume" | Zero hedge words in a full requirements rep | closing | Sitting 7 | hedges 0 ✓, question ✗ |
| G5 | **Second-order numbers** — rate → bytes → store → consequence | The constraining number *and* what it forces, 4/5 | **closed** | Sitting 7 | ✅ in mixed |
| G6 | **Below the line = adjacent systems** — never the crux, never filler | Three exclusions, all adjacent systems, 4/5 | **closed** | Sitting 7 | ✅ in mixed |
| G7 | **FRs are user sentences** — no design decisions in minute two | Leaked FR rewritten + decision parked as a tradeoff, 4/5 | closing | Sitting 7 | 2/4 in mixed |

## Sittings

### Sitting 1 — 2026-09-20 — G1 find the tension

Form drilled: *"**A** wants X. **B** wants Y. Conflict because Z. Resolved by W — at the cost of V."*

| # | Problem | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| 1 | [5 Ticketmaster](requirements-index.md#5-ticketmaster) | Scale vs correctness; "conflict because two users book same seat"; resolved by Redis lock + DB row lock | ⚠️ | Browsing (10M stale reads) vs booking (serialised) — same state, opposite guarantees; resolved by cached map + **hold with TTL**, waiting room bounds the CP path |
| 2 | [24 Robinhood](requirements-index.md#24-robinhood) | *skipped* | ❌ | Lossy price fan-out vs lossless order ledger — one login, two durability guarantees; two systems: conflated pub/sub + durable log/state machine |
| 3 | [39 Autocomplete](requirements-index.md#39-search-autocomplete) | Latency vs freshness; precompute, choose latency, freshness ~15 min | ✅ | Same; sharpen: **removals** must land in seconds while additions may be stale |
| 4 | [28 FB Post Search](requirements-index.md#28-fb-post-search) | *skipped* | ❌ | Searchable < 1 min vs sort by engagement at 100k likes/s — likes cannot re-index documents; keep the score out of the document |
| 5 | [13 Rate Limiter](requirements-index.md#13-rate-limiter) | 10 ms vs correct count; "might… some incorrectness tolerable"; no resolution | ⚠️ | 10 ms wants local, correctness wants global; local counters + async reconciliation, slight over-admit as the stated cost; fails open |

**Pass rate:** 1/5. Notes: skips count as ❌ — in a loop you get the problem you get. Resolutions named mechanisms (Redis, row lock) instead of the architectural move. Every ideal ends with the *cost* of the resolution.

**Re-drill:**

| # | Problem | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| R1 | [14 Online Auction](requirements-index.md#14-online-auction) | *skipped* | ❌ | Last-10-s bid burst wants one row serialised; millions watching want it fast and stale; resolved by a single writer per auction + CAS on `current_max`, price pushed via SSE |
| R2 | [33 Notification System](requirements-index.md#33-notification-system) | Latency vs exactly-once; at-least-once + retry; "latency over correctness" | ❌ | Wrong pair — the line had it: 5k/s **campaign surge** vs **OTP in 5 s** in one pipeline; resolved by separate pipelines with separate quotas (priority *isolation*, not ordering) |
| R3 | [26 Web Crawler](requirements-index.md#26-web-crawler) | *skipped* | ❌ | 25k pages/s aggregate vs per-host politeness — throughput comes only from breadth; front (priority) + back (per-host) queues, frontier checkpointed |

**Verdict:** G1 → **open**. Pass 1/5 + 0/3. Root cause: treating the drill as recall — skipping unseen problems instead of reading the line for the two numbers that pull against each other; on attempted items, reaching for known material (at-least-once) instead of the line. **Revisit** after reading 14, 24, 26, 28: redo R1 guided (a/b/c questions), then a fresh batch.

### Sitting 2 — 2026-09-20 — G2 CAP per operation

Form drilled: *"**[Op A]** is AP — tolerating N because …. **[Op B]** is CP — because what breaks."* Modelled on 4 Local Delivery.

| # | Problem | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| 1 | [29 Payment](requirements-index.md#29-payment-system) | History accurate, exactly-once, freshness < 15 min | ⚠️ | **Charge** is CP (idempotency key + state machine; double-charge is unrecoverable). **History** is AP, minutes of lag from a projection |
| 2 | [6 Instagram](requirements-index.md#6-instagram) | Fan-out on write vs read design; celebrity case | ❌ | Uniformly AP — **feed read** tolerates 2 min; **post** needs durability not consistency; *no CP operation — say so* |
| 3 | [5 Ticketmaster](requirements-index.md#5-ticketmaster) | Seat map read available < 50 ms vs booking correct; locks block reads; TTL locks | ✅ | Same — add the labels: map AP (seconds stale, clean failure), booking CP via hold with TTL |
| 4 | [27 Ad Click](requirements-index.md#27-ad-click-aggregator) | Redirect < 100 ms vs no click lost; async via Kafka | ⚠️ | G1 answer, not G2. **Redirect** AP (served even if logging is down). **Metrics** eventual — minutes of lag — but *complete*: eventual ≠ lossy |
| 5 | [40 Doc Search](requirements-index.md#40-full-text-document-search) | *skipped* | ❌ | **Search** AP (index lags by refresh interval). **Doc store** is durable truth; index derived/rebuildable; exhaustive queries via PIT cursor under a weaker SLO |

**Pass rate:** 1/5. Notes: content present, form absent — the words AP / CP / eventual / strong never appeared. Answered with designs (#2) or the G1 tension (#4) instead of the operation-guarantee sentence. *Fill the template.*

**Re-drill:** 22 Top K · 18 Job Scheduler · 10 WhatsApp — template fill-in — *deferred by user; revisit with G1.*

**Verdict:** G2 → **open**. Pass 1/5.

### Sitting 3 — 2026-09-20 — G3 the "never" NFR

Form drilled: *"**[Thing] must never [happen]** — because [cost]."* Said before any scale NFR. Modelled on 27 Ad Click.

| # | Problem | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| 1 | [18 Job Scheduler](requirements-index.md#18-job-scheduler) | At-least-once; retry w/ backoff + jitter; assumes idempotent | ⚠️ | *A due job must never be skipped; a retry must never run its side effect twice — a skipped payroll, a double payment.* Idempotency key `(job_id, fire_time)` |
| 2 | [33 Notification](requirements-index.md#33-notification-system) | At-least-once; retry w/ backoff; late/out-of-order eventually processed | ⚠️ | *An OTP must never be dropped; a user must never get the same notification twice — locked-out user, carrier block.* Dedupe key end to end |
| 3 | [26 Web Crawler](requirements-index.md#26-web-crawler) | Durable/resumable; "exactly-once stateful to avoid rework" | ⚠️ | *Must never restart from zero; must never exceed a host's rate limit — no slack in 5 days, IP ban ends the crawl.* At-least-once fetch is fine |
| 4 | [36 Durable Execution](requirements-index.md#36-durable-execution-engine) | Payment API exactly-once; saga/compensation; other activities idempotent | ✅ | *A completed step must never re-execute on replay — it was a charge.* Engine records completion exactly-once; the call itself is at-least-once + idempotency key |
| 5 | [22 Top K](requirements-index.md#22-youtube-top-k) | Durable, queryable at any time; late/out-of-order handled within window | ❌ | Missed **exact**: *a view must never be dropped or double-counted, and top-K must never be approximated — the ranking is the product* |

**Pass rate:** 1/5 form, ~4/5 content. Notes: answered in mechanisms (backoff, jitter ×2) instead of requirements; **cost clause missing in all five**. Warm-up G1 item not answered.

**Re-drill:**

| # | Problem | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| R1 | [29 Payment](requirements-index.md#29-payment-system) | "Never double charged for the same order — almost impossible to recover from" | ✅ | Same. Sharpen: at-least-once attempt + idempotency key = effectively once |
| R2 | [10 WhatsApp](requirements-index.md#10-whatsapp) | "Never miss any message within the 30-day window" | ⚠️ | Form ✓, no cost; missed that after 30 days the message must be *deleted* — retention is a deletion requirement |
| R3 | [2 Dropbox](requirements-index.md#2-dropbox) | "Never re-sync a file already uploaded — avoid rework" | ❌ | Dedupe is an optimisation. *A file must never be lost or corrupted (only copy); an upload must never restart from zero (50 GB on a flaky link)* |

**Verdict:** G3 → **closing**. First clean ✅ on form (R1). Cost clause still dropped 2/3. Warm-up next sitting.


### Sitting 4 — 2026-09-20 — G4 ask once, then commit

Form drilled: *"**Q:** one question that changes the design. **If yes →** bold sentence. **If no →** bold sentence."* Modelled on 30 Metrics late data.

**Warm-up G3** — 5 Ticketmaster: "never book the same seat twice — cost: operational complexity" → ⚠️ form ✓, cost wrong (cost of *violation*: two people, one seat).

| # | Prompt | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| 1 | [1 Bit.ly](requirements-index.md#1-bitly-url-shortener) | "Permanent or temporary move? My opinion: temporary" | ⚠️ | Wrong question (design, not requirement), self-answered. **Q:** expire/delete/analytics? yes → 302 revocable, short TTL; no → 301 permanent |
| 2 | [22 Top K](requirements-index.md#22-youtube-top-k) | "K bounded to a few thousand? yes" | ⚠️ | Good Q, one branch. no → sort over all videos, OLAP, seconds |
| 3 | [13 Rate Limiter](requirements-index.md#13-rate-limiter) | "100% accurate or margin ok? yes, margin" | ⚠️ | Good Q, one branch. exact → central atomic counter sharded by key, pay the hop |
| 4 | [10 WhatsApp](requirements-index.md#10-whatsapp) | "Retention? yes, 30 days" | ⚠️ | Fair Q, not the one. **Q:** E2E encrypted? yes → no server search/dedupe, client fan-out; no → server can |
| 5 | [27 Ad Click](requirements-index.md#27-ad-click-aggregator) | "Approx or exact? exact" | ⚠️ | Skipped the hint. **Q:** same user, two clicks in 1 s — one or two? one → dedupe on (user, ad) window; two → dedupe on click_id only |

**Pass rate:** 0/5 on form — every answer had exactly one branch; two were self-answered. The gap precisely: ask, then assume the wanted answer.

**Re-drill:**

| # | Problem | Your answer (1 line) | Score | Ideal |
|---|---|---|---|---|
| R1 | [6 Instagram](requirements-index.md#6-instagram) | "Feed delay < 15 min ok? yes → < 15 min; no → < 10 s" | ⚠️ | Both branches ✓; wrong lever. **Q:** chronological or ranked? chrono → precomputed per-user list, fan-out on write; ranked → candidates + scoring at read |
| R2 | [18 Job Scheduler](requirements-index.md#18-job-scheduler) | "Restart partially executed jobs? yes → idempotent/dup ok; no → user decides" | ⚠️ | Both branches ✓; "user decides" is a punt. **Q:** backfill or skip missed firings? backfill → run all in order, dedupe `(job_id, fire_time)`, cap burst; skip → next_run from now. Staff: per-job policy |
| R3 | [2 Dropbox](requirements-index.md#2-dropbox) | "Max size? small → single HTTP call; 50 GB → multipart" | ✅ | Same |

**Verdict:** G4 → **closing**. 3/3 both branches (form landed), 1/3 branches carrying a consequence. Next: each branch must end in *what it forces*.


### Sitting 5 — 2026-09-20 — G5 second-order numbers

Form drilled: *rate → × bytes → per day / total → fits where? → "so the design must …"*. Modelled on 30 Metrics (5M/s → 1 GB/s → 86 TB/day → compression + rollups + tiers). Warm-up (G3+G4 on Crawler) not answered.

| # | Problem | Your answer (1 line) | Score | Ideal chain |
|---|---|---|---|---|
| 1 | [1 Bit.ly](requirements-index.md#1-bitly-url-shortener) | Durable store, cache, "shard by geography" — no numbers | ❌ | 1B × 500 B = **500 GB** → one node + replicas → *no sharding*; 100:1 → cache + edge |
| 2 | [6 Instagram](requirements-index.md#6-instagram) | Push fan-out, celebrity pull, "1000:1" — no numbers | ❌ | 100M × 200 = **230k fan-out writes/s** vs 58k reads/s → writes exceed reads 4:1 → bounded window, skip inactive, threshold derived |
| 3 | [22 Top K](requirements-index.md#22-youtube-top-k) | "Fits in memory", flush to DB — no number | ⚠️ | 1B × 16 B = **16 GB** (64 GB w/ overhead) → one machine → in-memory counters, snapshot + replay |
| 4 | [26 Web Crawler](requirements-index.md#26-web-crawler) | S3 raw, checkpoint, ES, "latency is bottleneck" — no numbers | ❌ | 23k pages/s × 100 KB = **2.3 GB/s ≈ 20 Gbps**, **1 PB**/5 days → HTML only; bandwidth + storage cost is the wall |
| 5 | [2 Dropbox](requirements-index.md#2-dropbox) | Presigned, "10 KB chunks", chunk table — no numbers | ❌ | 50 GB / 4 MB = **12.5k chunks**; 2.5M rows/day, 1B/yr → chunk table is the big one, partition by file_id. 10 KB → 5M chunks/file: the number rejects it |

**Pass rate:** 0/5. Notes: zero arithmetic in five answers; three invented numbers (1000:1, 10 KB, geo-shard) that the real chain contradicts. Purest instance of the gap.

**Re-drill:**

| # | Problem | Your answer (1 line) | Score | Ideal chain |
|---|---|---|---|---|
| R1 | [13 Rate Limiter](requirements-index.md#13-rate-limiter) | 100M × 10 × 64 B = 64 GB → fits a node → Redis; 1-min window → no durable store | ✅ | Same. Sharpen: 1M writes/s is one Redis node's ceiling → shard by key or local-first. GB not Gb |
| R2 | [10 WhatsApp](requirements-index.md#10-whatsapp) | "30 TB undelivered → distributed DB, shard" | ⚠️ | 10 MB/s → 864 GB/day → 26 TB/30 d → **×10% ≈ 2.6 TB**. Off 10×. Consequence: inbox partitioned by user_id, TTL 30 d, delete on ack |
| R3 | [19 FB Live Comments](requirements-index.md#19-fb-live-comments) | *skipped* | ❌ | 1M × 1k/s × 200 B = **200 GB/s** → impossible → coalesce per viewer/s, sample/rank hot videos, dispatcher tree. Pure arithmetic — no prior knowledge needed |

**Verdict:** G5 → **closing** (weak). Chains in 2/3. Arithmetic slips (10×, Gb/GB). Skipped the one where arithmetic *is* the design.


### Sitting 6 — 2026-09-20 — G6 below the line + G7 FRs as user sentences

Modelled on 30 Metrics (logs/traces/ML out; alerting *in*; notification delivery out; "servers push" → "platform ingests", push/pull parked).

**Warm-up G5** — 27 Ad Click: "100M × 200 B × 365 → 700 GB → fits Postgres" → ⚠️ off 10× (**7.3 TB**); verdict flips: raw → S3, aggregates → OLAP. Second 10× slip in a row.

**Method taught for exclusions:** look in three places — (1) upstream/downstream systems, (2) the sibling that shares a noun, (3) the v2 feature — then test *"could someone else own it?"* (yes → out) and *"is it the hard part?"* (yes → never out). Hygiene (auth, security, GDPR, CI/CD) as one bundled phrase.

| # | Problem | Exclusions (a) | FR rewrite (b) | Score | Ideal |
|---|---|---|---|---|---|
| 1 | [21 Price Tracking](requirements-index.md#21-price-tracking-service) | Up: "Amazon" (dependency, not the system) ⚠️ · Down: auto-buy triggers ✅ · v2: "historical trend dashboard" ❌ — that *is* the product · sentence not given | "System refreshes price hourly" — still system + mechanism; parked push/pull then re-decided it | ⚠️ | Out: anti-bot/ToS handling as a platform, notification delivery, price prediction/deal ranking. FR: "users are alerted within 1 h of a price change"; parked: polling cadence (uniform vs tiered), source (scrape vs API vs extension) |
| 2 | [9 LeetCode](requirements-index.md#9-leetcode) | Up: "UI/code pad" ⚠️ (client, not upstream) · Down: AI code-quality eval ✅ · v2: premium problems ✅ · sentence ✓ | "Submit within 10 s; 5 s cooldown" ❌ — changed the number, invented a mechanism (cooldown), parked the wrong tradeoff | G6 ✅ · G7 ❌ | Out: auth/accounts, problem authoring/curation, payments/premium. FR: "users submit code and get a verdict within 5 s"; parked: container vs gVisor vs microVM, timeout budget |
| 3 | [17 YouTube](requirements-index.md#17-youtube) | Hygiene bundle (auth/DoS/spam) · likes/comments/views v2 · analytics v2 · sentence ✓ · tests not said aloud | "Users watch at a quality that adapts to bandwidth" ✅; "storage 5×" is a cost of one side, not the fork (ladder eager vs lazy, HLS vs DASH) | G6 ✅ · G7 ✅ | Out: recommendations, comments/likes, live streaming. FR: "users can watch uploaded video on any bandwidth"; parked: rendition ladder, HLS vs DASH, eager vs lazy transcode |
| 4 | [35 RAG](requirements-index.md#35-rag-application) | *deferred* | | — | Out: the LLM itself, document authoring, auth/ACL source. FR: "users get answers grounded in their documents, with citations"; parked: chunk size/strategy, vector store choice |

**G7 trick taught:** find the mechanism noun in the leaked FR, delete it, keep the promise + number; the parked tradeoff is *that noun vs its alternatives*.

**Verdict:** G6 → **closing** (3/3 attempted; #4 RAG deferred). G7 → **closing** (1/3; #4 deferred).


### Sitting 7 — 2026-09-20 — Mixed review: 30 Metrics Monitoring, full rep, no templates

| Gap | Score | Held / collapsed without a template |
|---|---|---|
| G7 FRs | ⚠️ | 2/4 user sentences; "published from the server" leaks push |
| G6 Below the line | ✅ | Notification delivery correctly cut (baseline had *alerting* out); no sentence, no hygiene bundle, logs/tracing unused |
| G5 Numbers | ✅ | 5M/s → 5 TB/day → consequence; arithmetic right. Consequence half-right (ingest at 1 min ✗ → roll up ✓); Iceberg ✗ (batch format, not a TSDB) |
| G3 Never | ❌ | Late/dup data mentioned but as a self-contradicting jumble, no *never*, no cost, said last. Collapsed without the template |
| G2 CAP per op | ⚠️ | Split present (alerts 1 min / dashboards lag) — structure ✓, vocabulary ✗; "highly available" still system-wide |
| G1 Tension | ⚠️ | Form produced unprompted ✓; wrong pair (resolution vs cost, not stale-dashboards vs reliable-alerts); resolution wrong (grain, not rollups) |
| G4 Ask | ⚠️ | Zero hedge words ✓ (baseline: four); no question — push/pull, late data, **cardinality** all available; cardinality absent from the whole rep |

**Versus baseline on the same problem:** four gaps moved (G6, G5, G2 structure, G4 hedges). Held: G5, G6, G4-hedges. Collapsed: G3; G1/G2 vocabulary.

**Verdicts:** G5 → **closed** (chain held; watch units). G6 → **closed**. G4 → **closing** (hedges gone; question still missing). G7 → **closing**. G2 → **closing** (structure held). G1 → **open**. G3 → **open** (regressed).

**Reopen loop next:** G3 mini-sitting (never + cost, no template, said *first*) · G1 mini-sitting (the crux pair, not the secondary one) · deferred re-drills (G1 R1–R3, G2 R1–R3, G6/G7 #4 RAG) · second mixed review on 27 Ad Click.

### Sitting 8 — 2026-09-20 — Restatement + final summary quiz

Restated for 30 Metrics Monitoring, side by side: the never-sentence (*an alert must never be missed or fire/resolve on a partial window — a missed page is an outage nobody sees*) and the tension (*dashboards want cheap rolled-up reads, alerts want every raw point on time; same ingest; one write path → two consumers with different guarantees; cost: the evaluator as its own tier*). Noted that the never-sentence is the A-side of the tension.

Session closed by user with a final summary quiz across all seven gaps and the three DE anchors — *results below*.

## Baseline — 2026-09-20

The three rounds that produced the gaps, and what each cost.

| # | What happened | What it cost | Evidence |
|---|---|---|---|
| G1 | Never named the tension; skipped the "what conflicts?" bonus all three times | The crux — a generic pipeline instead of *this* design | 27: lossless+idempotent vs fast redirect · 22: exact vs 1M/s (both facts stated, never connected) · 30: stale dashboards vs reliable alerts |
| G2 | NFRs as adjectives — "availability", "eventual consistency" — never *which operation* | The senior signal; zero per-op CAP sentences given | 30: dashboards AP / alerts reliable · 27: redirect AP / counts eventual-but-lossless |
| G3 | Missed the correctness NFR every time; optimised scale + latency only | The NFR the design is built around | 27: lossless, idempotent · 22: exact (waffled), durable · 30: late data dismissed as "no importance" |
| G4 | Hedged instead of asked — "tolerable?", "best-effort?", "assume", "can be ignored" | Reads as uncertainty; a question mark cannot be graded | Saw the ambiguity every time (good), then guessed softly instead of asking once |
| G5 | First multiplication only — 10k/s, 1M/s, 500k×20 — then stopped | The second number is the one that constrains the design | 22: 64 GB counters fit in RAM · 30: 1 GB/s → 80 TB/day → rollups · 27: 100M/day |
| G6 | Below the line held filler (security) or the crux itself (alerting) | Signals the shape was not seen | Below the line = adjacent systems: notification delivery, tracing, fraud |
| G7 | Design decisions leaked into FRs — "server should push", "custom slice/dice" | Commits in minute two to what should be traded off in minute twenty | 30: push vs pull · 27: arbitrary dimensions vs pre-aggregation |
