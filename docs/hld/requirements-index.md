# Requirements Index — FR / NFR / out-of-scope for every problem

[← HLD index](README.md) · [All docs](../README.md) · [Interview framework](00-interview-framework.md)

---

The first six minutes of an HLD interview are requirements, and they are graded. This page
holds the **ideal requirement set for every problem in the set** — the three-to-four
functional requirements worth writing on the board, the non-functional requirements *with
the number attached*, and the below-the-line list you say out loud so the interviewer knows
you chose the scope rather than forgot it.

**Every row is a superset.** On **2026-09-20** all **32** HelloInterview breakdowns — including
the eleven whose NFR lists sit behind the premium gate — were retrieved in full and merged
with the local notes. Each cell now lists the **published** requirements first, in
HelloInterview's own wording, followed by anything the local notes add, marked `°`. Nothing
from either source was dropped: what the source states and what the notebook adds are both
here, and you can always tell which is which.

That distinction is the point. An unmarked line is quotable — the interviewer's own
framing, and the phrasing they are listening for. A `°` line is yours: usually a good
instinct, sometimes a better sentence than the source's, but never something to present as
though the question handed it to you.

Merging surfaced four corrections that change a number you would say out loud — the News
Aggregator feed budget (**200 ms**, not 500), the chess move budget (**200 ms**, not 100),
the ChatGPT time-to-first-token budget (**~500 ms**, not 1 s) and the job-scheduler
tolerance (**2 s**, not ~1 s). Three rows carried invented scale figures (Online Auction,
Robinhood, Flash Sale), and Robinhood was missing a published requirement outright. Those
eleven sections each carry a note recording what changed.

## How to read a row

Every problem without a ★ is backed by a HelloInterview breakdown that has been
retrieved in full, so there is no longer a mark for *"published"* — that is the
baseline now. The marks that remain are the ones that still tell you something.

| Mark | Meaning |
|:---:|---|
| ° | Present **only in the local notes**, beyond HelloInterview's published list — yours to volunteer, not to quote |
| ★ | Problem only exists locally — no HelloInterview breakdown |
| ⌀ | No local notes file yet |

## What makes a requirement "ideal"

Four rules, and every row below obeys them:

1. **Functional requirements are user sentences, and there are three of them.** *"Users should
   be able to X"* — not "a service that does X". Three or four, ordered by what you will
   build first. A fifth is usually the one to push below the line.
2. **Every non-functional requirement carries a number or a choice.** "Low latency" is not a
   requirement; "`< 200 ms` p99 for the redirect" is. "Highly available" is not a
   requirement; "**availability over consistency**, tolerating 1 minute of staleness" is.
   Run the [SCALE checklist](00-interview-framework.md#nfr-checklist-scale-for-cloud-designs)
   and keep only the three or four entries that actually bite.
3. **One CAP sentence, and it names the operation, not the system.** Ticketmaster is AP for
   browsing and CP for booking. Systems that are uniformly one or the other are the boring
   minority.
4. **Below the line is stated aloud.** Saying "I'm dropping auth, payments and moderation"
   costs eight seconds and buys the rest of the hour. Note that *security, GDPR, CI/CD,
   backups and monitoring* are below-the-line filler in almost every one of these problems —
   they are real engineering concerns that are never the interesting part of a 60-minute
   design, so name them as excluded rather than listing them as NFRs.

## The crux, and what each level is expected to do with it

Every problem below carries a **Crux** — the one thing the problem exists to test, stated in
a sentence — and, under the requirements table, a second table with three columns that unpack it:

| Column | What goes in it |
|---|---|
| **Below the line** | One line under the requirements table — the scope you are dropping, said aloud. |
| **Key bottlenecks (crux)** | The concrete constraints that must be solved for the crux to hold — the numbers and requirements that collide. If you have not named these, you have not found the problem. |
| **Key tradeoffs** | The alternatives you are expected to weigh, each with the reason one side wins here. An interviewer hears these as "considered and rejected", which is the senior signal. |
| **Key design expected** | The pipeline and data model that clears the bar — components, keys, what flows where. |

Mid and senior expectations are merged in each cell; the **Staff+** points sit in
`(staff: …)` brackets at the end. The levels are **cumulative**: a staff answer contains the
senior answer, which contains the mid answer. Nobody is impressed by the bracketed points if
the unbracketed ones are missing.

| Level | What the interviewer is deciding | The failure that costs the level |
|---|---|---|
| **Mid** | Can you produce a **complete, working design**? Right components, data flowing end to end, the happy path holds, numbers on the board. | Gaps. A box with no data model, an API nobody calls, a requirement silently dropped. |
| **Senior** | Do you find **the one constraint that decides the architecture**, and design around it with justified trade-offs? Alternatives named and rejected for reasons. Failure and scale handled on demand. | Breadth without depth: a plausible diagram, then no answer to "what happens at 10×", or a trade-off asserted with no alternative considered. |
| **Staff+** | Do you **reframe the problem**, name the invariant, and own the consequences — cost, operability, evolution, and what you are deliberately sacrificing? Do you say which decisions are the business's, not yours? | Solving the problem as stated while missing that it was the wrong problem; or a technically perfect design nobody could operate, afford, or change. |

Two things separate the Staff+ points in almost every problem below, and they are worth naming
because they are learnable: **saying what you are sacrificing and when it breaks**, and
**handing product decisions back to the product** — fairness, retention, whether trading
continues on a stale price — instead of quietly deciding them in code.

## The index

| # | Problem | Notes | Breakdown | Headline scale | The NFR that decides the design |
|---:|---|:---:|:---:|---|---|
| 1 | [Bit.ly](#1-bitly-url-shortener) | [notes](01-bitly-url-shortener.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/bitly) | 1B URLs, 100M DAU, 100:1 read | Redirect `< 100 ms`, AP |
| 2 | [Dropbox](#2-dropbox) | [notes](02-dropbox.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/dropbox) | files to 50 GB | Large-file upload/download, resumable |
| 3 | [Yelp](#3-yelp) | [notes](03-yelp.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/yelp) | 100M DAU, 10M businesses | Geo search `< 500 ms`, AP |
| 4 | [Local Delivery](#4-local-delivery-service-gopuff) | [notes](04-local-delivery-service.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/gopuff) | 10k DCs, 100k SKUs, 10M orders/day | Availability query `< 100 ms` **and** strongly consistent ordering |
| 5 | [Ticketmaster](#5-ticketmaster) | [notes](05-ticketmaster.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/ticketmaster) | 10M users on one event | CP for booking, AP for browsing |
| 6 | [Instagram](#6-instagram) | [notes](06-instagram.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/instagram) | 500M DAU, 100M posts/day | Feed `< 500 ms` despite celebrity fan-out |
| 7 | [FB News Feed](#7-fb-news-feed) | [notes](07-fb-news-feed.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/fb-news-feed) | 2B users, unbounded follows | Feed `< 500 ms`, 1 min staleness allowed |
| 8 | [Tinder](#8-tinder) | [notes](08-tinder.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/tinder) | 20M DAU × 100 swipes | Consistent swipes; never re-show a swiped profile |
| 9 | [LeetCode](#9-leetcode) | [notes](09-leetcode.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/leetcode) | 100k-user competition | Isolated execution of hostile code, result `< 5 s` |
| 10 | [WhatsApp](#10-whatsapp) | [notes](10-whatsapp.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/whatsapp) | billions of users, ~100k writes/s | Guaranteed delivery incl. 30 days offline |
| 11 | [Strava](#11-strava) | [notes](11-strava.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/strava) | 10M concurrent activities | Works with **no network** |
| 12 | [Distributed Cache](#12-distributed-cache) | [notes](12-distributed-cache.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/distributed-cache) | 1 TB, 100k RPS | `< 10 ms` get/set, LRU under memory pressure |
| 13 | [Rate Limiter](#13-rate-limiter) | [notes](13-rate-limiter.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/distributed-rate-limiter) | 1M req/s, 100M DAU | `< 10 ms` added latency, eventual consistency ok |
| 14 | [Online Auction](#14-online-auction) | [notes](14-online-auction.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/online-auction) | 10M concurrent auctions | Strong consistency on the highest bid; no bid dropped |
| 17 | [YouTube](#17-youtube) | [notes](17-youtube.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/youtube) | 1M uploads, 100M watches/day | Stream 10s-of-GB video on poor bandwidth |
| 18 | [Job Scheduler](#18-job-scheduler) | [notes](18-job-scheduler.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/job-scheduler) | 10k jobs/s | Fires within **2 s** of due time; at-least-once |
| 19 | [FB Live Comments](#19-fb-live-comments) | [notes](19-fb-live-comments.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/fb-live-comments) | millions of videos, 1000s comments/s each | Broadcast `< 200 ms` end to end |
| 20 | [News Aggregator](#20-news-aggregator) | [notes](20-news-aggregator.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/google-news) | 100M DAU, spikes to 500M | AP — stale news beats no news; feed `< 200 ms` |
| 21 | [Price Tracking](#21-price-tracking-service) | [notes](21-price-tracking-service.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/camelcamelcamel) | 500M products | Alert `< 1 h` of price change, on someone else's API budget |
| 22 | [YouTube Top K](#22-youtube-top-k) | [notes](22-youtube-top-k.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/top-k) | ~700k views/s | **Exact**, not approximate, within 1 min |
| 23 | [Uber](#23-uber) | [notes](23-uber.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/uber) | 100k requests from one location | One driver, one ride — strong consistency in matching |
| 24 | [Robinhood](#24-robinhood) | [notes](24-robinhood.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/robinhood) | 20M DAU × 5 trades/day, 1000s of symbols | CP orders + price fan-out off a **costly** exchange feed |
| 25 | [Google Docs](#25-google-docs) | [notes](25-google-docs.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/google-docs) | 100 concurrent editors/doc | **Convergence**, not consistency; local echo instant |
| 26 | [Web Crawler](#26-web-crawler) | [notes](26-web-crawler.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/web-crawler) | 10B pages in 5 days ≈ 25k pages/s | Politeness + resumability |
| 27 | [Ad Click Aggregator](#27-ad-click-aggregator) | [notes](27-ad-click-aggregator.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/ad-click-aggregator) | 10k clicks/s peak, 100M/day | Idempotent, lossless, near-real-time |
| 28 | [FB Post Search](#28-fb-post-search) | [notes](28-fb-post-search.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/fb-post-search) | 10k posts/s, 100k likes/s, 10k searches/s | Searchable `< 1 min`; **all** posts discoverable |
| 29 | [Payment System](#29-payment-system) | [notes](29-payment-system.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/payment-system) | 10k+ TPS, bursty | Integrity despite **asynchronous** payment networks; nothing lost |
| 30 | [Metrics Monitoring](#30-metrics-monitoring) | [notes](30-metrics-monitoring.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/metrics-monitoring) | 5M metrics/s from 500k servers | Alert `< 1 min`; dashboards may be eventually consistent |
| 31 | [Online Chess](#31-online-chess) | [notes](31-online-chess.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/online-chess) | 500k concurrent games = 1M connections | Move `< 200 ms`; **pause, don't diverge** |
| 32 | [ChatGPT](#32-chatgpt) | [notes](32-chatgpt.md) ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/chatgpt) | 200M DAU, 20k prompts/s, 120k live streams | **TTFT `< ~500 ms`**, and GPUs rationed across free/Plus/Pro |
| 33 | [Notification System](#33-notification-system) | [notes](33-notification-system.md) | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/notification-system) | 10M/day, 5k/s campaign surge | OTPs within **5 s** even mid-surge |
| 34 | [Game Leaderboard](#34-game-leaderboard) ★ | [notes](34-game-leaderboard.md) ⌀ | — | 100M players, 10k scores/s | Own-rank lookup is the expensive query, not top-N |
| 35 | [RAG Application](#35-rag-application) ★ | [notes](35-rag-application.md) | — | corpus in the millions of chunks | Answers attributable to retrieved chunks |
| 36 | [Durable Execution Engine](#36-durable-execution-engine) ★ | [notes](36-durable-execution-engine.md) | — | millions of open, mostly idle executions | Exactly-once state transitions; replayable for years |
| 37 | [Local Agent Runtime](#37-local-agent-runtime) ★ | [notes](37-local-agent-runtime.md) | — | one machine, one user | The model never executes anything |
| 38 | [Metrics Aggregation Platform](#38-metrics-aggregation-platform) ★ | [notes](38-metrics-aggregation-platform.md) | — | 10k tenants, N provider APIs | Provider API budget is the scarce resource |
| 39 | [Search Autocomplete](#39-search-autocomplete) ★ | [notes](39-search-autocomplete.md) | — | ~220k peak QPS | p99 `< 50 ms`; AP except **removals** |
| 40 | [Document Search](#40-full-text-document-search) ★ | [notes](40-document-search.md) | — | 100M docs, 5k docs/s indexed | p99 `< 200 ms` top-K; index is rebuildable |
| — | [Flash Sale](#flash-sale-no-local-notes) | ⌀ | [HI](https://www.hellointerview.com/learn/system-design/problem-breakdowns/flash-sale) | millions arriving in seconds (**no figures published**) | Never oversell, and be **demonstrably fair** |

*Problems 15 and 16 do not exist in the source notebook. Flash Sale has a HelloInterview
breakdown but no local file yet. Breakdown list current as of 2026-09-19.*

### The crux of each problem, in one line

If you read nothing else on this page before a loop, read this column. Each links to the
full requirement set and the level breakdown.

| # | Problem | Crux |
|---:|---|---|
| 1 | [Bit.ly](#1-bitly-url-shortener) | Short-code generation without read-before-write; the redirect at the edge |
| 2 | [Dropbox](#2-dropbox) | Chunking — it buys resumability, dedupe and delta sync in one move |
| 3 | [Yelp](#3-yelp) | The geo index choice, and a rating that is precomputed not aggregated |
| 4 | [Local Delivery](#4-local-delivery-service-gopuff) | One inventory, two paths: stale-fast reads, transactional writes |
| 5 | [Ticketmaster](#5-ticketmaster) | The hold with a TTL, and admission control at the on-sale moment |
| 6 | [Instagram](#6-instagram) | Fan-out on write vs read, and where the celebrity threshold sits |
| 7 | [FB News Feed](#7-fb-news-feed) | Unbounded followers make hybrid fan-out mandatory, not clever |
| 8 | [Tinder](#8-tinder) | The growing exclusion set, and one mutual swipe producing exactly one match |
| 9 | [LeetCode](#9-leetcode) | Running hostile code safely inside a 5-second budget |
| 10 | [WhatsApp](#10-whatsapp) | Delivery to an offline client, with retention as a deletion requirement |
| 11 | [Strava](#11-strava) | Offline-first — the phone is the source of truth |
| 12 | [Distributed Cache](#12-distributed-cache) | Consistent hashing and hot keys; LRU is the easy half |
| 13 | [Rate Limiter](#13-rate-limiter) | Where the counter lives at 10 ms, and what it does when it is down |
| 14 | [Online Auction](#14-online-auction) | Serialising bids on one row while broadcasting to millions |
| 17 | [YouTube](#17-youtube) | The transcode ladder and segment-level delivery |
| 18 | [Job Scheduler](#18-job-scheduler) | Durable timers plus idempotent at-least-once execution |
| 19 | [FB Live Comments](#19-fb-live-comments) | One write to a million sockets, plus a joinable backlog |
| 20 | [News Aggregator](#20-news-aggregator) | Ingest from sources you do not control; a spike-proof read path |
| 21 | [Price Tracking](#21-price-tracking-service) | Spending a finite crawl budget where it pays |
| 22 | [YouTube Top K](#22-youtube-top-k) | Exactness at 700k/s — split aggregation from serving |
| 23 | [Uber](#23-uber) | Matching as a serialised decision over a geo index rewritten every few seconds |
| 24 | [Robinhood](#24-robinhood) | Two systems: lossy price fan-out, lossless order ledger |
| 25 | [Google Docs](#25-google-docs) | Convergence — the server sequences, it does not lock |
| 26 | [Web Crawler](#26-web-crawler) | Politeness caps per-host rate, so throughput comes from breadth |
| 27 | [Ad Click Aggregator](#27-ad-click-aggregator) | Lossless idempotent ingest feeding a fast view and an authoritative one |
| 28 | [FB Post Search](#28-fb-post-search) | An index that absorbs 100k like-updates/s without reindexing |
| 29 | [Payment System](#29-payment-system) | Idempotency, a state machine, and reconciliation |
| 30 | [Metrics Monitoring](#30-metrics-monitoring) | One ingest, two guarantees — stale dashboards, reliable alerts |
| 31 | [Online Chess](#31-online-chess) | Server-authoritative live state, and failing it over |
| 32 | [ChatGPT](#32-chatgpt) | GPU scheduling; the rest is CRUD with streaming |
| 33 | [Notification System](#33-notification-system) | Fairness — a campaign must never delay a password reset |
| 34 | [Game Leaderboard](#34-game-leaderboard) | Rank, not top-N |
| 35 | [RAG Application](#35-rag-application) | Retrieval quality and evaluation; the LLM call is the easy part |
| 36 | [Durable Execution](#36-durable-execution-engine) | State rebuilt by deterministic replay — and versioning mid-flight |
| 37 | [Local Agent Runtime](#37-local-agent-runtime) | The trust boundary: model proposes, harness disposes |
| 38 | [Metrics Aggregation](#38-metrics-aggregation-platform) | Correctness and repairability on someone else's API budget |
| 39 | [Autocomplete](#39-search-autocomplete) | Ranking and freshness at 220k QPS in 50 ms |
| 40 | [Document Search](#40-full-text-document-search) | The indexing unit, and two SLOs for two query shapes |
| — | [Flash Sale](#flash-sale-no-local-notes) | Admission control — keep the traffic away from the row |

---

## 1. Bit.ly (URL Shortener)

[notes](01-bitly-url-shortener.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/bitly)

**Crux** — A design small enough to finish in twenty minutes, whose entire grade is **how short codes are generated** and how the read path is served.

| Functional | Non-functional |
|---|---|
| 1. Users can submit a long URL and receive a shortened version<br>2. Optionally, users can specify a **custom alias** ("short.ly/my-custom-alias")<br>3. Optionally, users can specify an **expiration date**<br>4. Users can access the original URL by using the shortened URL | 1. **Uniqueness** — each short code maps to exactly one long URL<br>2. Redirection with minimal delay — **`< 100 ms`**<br>3. Reliable and available **99.99 %** of the time (availability > consistency)<br>4. Scale to **1B shortened URLs and 100M DAU**<br>5. ° Durable — a mapping is never lost<br>6. ° Read:write is heavily skewed. HI's prose says ~**1000:1** clicks per create; the notes assume 100:1 (~10k reads/s, ~100 writes/s) |

**Below the line** — User authentication and account management · analytics on link clicks (counts, geographic) · °link editing and deletion · °custom domains · *NFR below the line:* consistency of real-time analytics · advanced security — spam detection and malicious-URL filtering (°notes call this malware/phishing scanning)

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Generate a unique 7-char code at ~100 writes/s with **no read-before-write** (hash-and-retry collides at 1B rows)<br>- Redirect `< 100 ms` at ~10k reads/s — the mapping must be one cache hit, ideally at the edge<br>- (staff: id allocation must survive a partition or host loss without duplicates — leased ranges, not a global counter) | - Random hash + retry vs counter/ranged ids — hash is simple but collision-prone and needs a read; counter needs coordination<br>- 301 vs 302 — 301 is cached by the browser (fastest, zero analytics, cannot revoke); 302 keeps abuse control and click data at the cost of a hop<br>- SQL vs KV — 1B rows of `code → url` is a KV shape; SQL only if custom aliases and expiry need uniqueness constraints<br>- (staff: edge caching vs consistency of takedowns — a short TTL is the compromise) | - `POST /urls` → id allocator (counter block or per-host range) → base62 → write `code, long_url, expiry` to KV/Dynamo<br>- `GET /{code}` → CDN/edge cache → Redis → DB; 302 with short TTL<br>- Hot-mapping cache; expiry enforced by TTL on read plus a sweeper<br>- (staff: takedown/malware path that invalidates the edge; link rot as the operating cost) |

> **Where the notebook differs.** [`01-bitly-url-shortener.md`](01-bitly-url-shortener.md)
> keeps *analytics* as a third functional requirement and then lists it under out-of-scope
> too. Pick one: drop analytics, and if the interviewer asks for it, answer it as a
> [22. Top-K](22-youtube-top-k.md) problem bolted on. Keeping it turns a 45-minute design
> into two designs.

## 2. Dropbox

[notes](02-dropbox.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/dropbox)

**Crux** — The file is larger than any request, so **chunking is the design** and everything else follows from it.

| Functional | Non-functional |
|---|---|
| 1. Users can upload a file from any device<br>2. Users can download a file from any device<br>3. Users can share a file with other users and view the files shared with them<br>4. Users can automatically sync files across devices | 1. Highly available — **availability over consistency**<br>2. Support files as large as **50 GB**<br>3. Secure and reliable — a lost or corrupted file can be recovered<br>4. Upload, download and sync times as fast as possible (low latency)<br>5. ° The 50 GB figure is what makes chunked, **resumable** transfer a requirement rather than a deep dive<br>6. ° Sync latency is measured *save → visible on the other device*, not per request |

**Below the line** — Editing files · viewing files without downloading them · °designing Blob Storage itself · *NFR below the line:* a per-user storage limit · file versioning · virus and malware scanning

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - A 50 GB file cannot be one request — upload must be **chunked and resumable** across network failures<br>- Sync must be *save → visible on other device* fast without polling the server for every file<br>- Metadata and blob must not disagree: a file is not 'uploaded' until every chunk is durable<br>- (staff: two devices editing the same file offline — a conflict that no protocol resolves for you) | - Upload through your servers vs presigned direct-to-S3 — direct saves egress and CPU but moves auth to the URL<br>- Fixed-size vs content-defined chunks — fixed is simple; CDC dedupes better after inserts<br>- Poll vs long-poll/WebSocket for sync — push is fresher, costs a connection per device<br>- (staff: cross-tenant dedupe by hash saves storage but turns the hash into a capability — close it or accept it) | - Client chunks (4 MB), fingerprints each, asks server which chunks are missing, uploads them via presigned URLs<br>- Metadata DB (files, chunks, versions) is the source of truth; commit the file row only after the last chunk lands<br>- Change log per user that clients tail (long-poll/WS) — delta sync = only changed chunks<br>- Sharing = ACL rows on the metadata; blobs are never re-copied<br>- (staff: conflict policy = last-writer-wins + conflicted copy, stated as a product decision; storage classes and egress costed) |

> **The 50 GB number is the whole design.** It forces chunking, a resumable protocol,
> presigned direct-to-S3 uploads and fingerprint-based dedupe. If you let the interviewer
> leave file size unspecified, you have lost the deep dive before it starts.

## 3. Yelp

[notes](03-yelp.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/yelp)

**Crux** — Which **geo index**, and why the rating is precomputed rather than aggregated on read.

| Functional | Non-functional |
|---|---|
| 1. Users can search for businesses by **name, location (lat/long) and category**<br>2. Users can view businesses and their reviews<br>3. Users can leave reviews on businesses — mandatory 1–5 star rating, optional text | 1. Low latency for search operations — **`< 500 ms`**<br>2. Highly available; **eventual consistency is fine**<br>3. Scale to **100M daily users and 10M businesses**<br>4. **One review per user per business** — published, but as an *added constraint* the interviewer introduces for senior+ candidates, not part of the base NFR list<br>5. ° Average rating is read far more than it is written, so it is precomputed rather than aggregated on read |

**Below the line** — Admins adding, updating and removing businesses · viewing businesses on a map · personalized recommendations · *NFR below the line:* user-data protection and GDPR · fault tolerance · spam and abuse protection

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Geo search `< 500 ms` over 10M businesses — plain `lat BETWEEN … AND lng BETWEEN …` cannot use a B-tree well<br>- Average rating read 1000× more than written — aggregating on read is the wrong shape<br>- One review per user per business must hold under a double-submit<br>- (staff: cell density skew — Manhattan vs Montana — breaks a fixed-cell geohash) | - Geohash vs quadtree vs PostGIS/S2 — geohash is a string prefix on any index; quadtree adapts to density; PostGIS avoids a second system<br>- Search in Postgres vs a search engine — one store is simpler; ES gives text + category + geo in one query but needs a CDC pipeline<br>- Precomputed rating (counter pair, eventually consistent) vs aggregate-on-read (exact, slow)<br>- (staff: sharding now vs never — 10M businesses fit in memory; refuse to shard prematurely) | - Business table with `geohash` column indexed; `sum_rating, review_count` maintained on write<br>- Search: geohash prefix range → filter by category/name → sort by distance; or ES with geo_point + text<br>- Review write: `PRIMARY KEY (user_id, business_id)` enforces one review; then update the counter pair<br>- (staff: CDC from Postgres into the search index; relevance ranking is the real product) |

> The scale is small enough that 10M businesses fit in memory. Say so — it kills the
> premature sharding conversation and buys time for the geo-index choice (geohash vs
> quadtree vs PostGIS), which is the actual question.

## 4. Local Delivery Service (GoPuff)

[notes](04-local-delivery-service.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/gopuff)

**Crux** — One inventory, **two paths with opposite guarantees**: a fast stale read and a transactional write.

| Functional | Non-functional |
|---|---|
| 1. Customers can query availability of items **deliverable in 1 hour**, by location — effective availability is the **union of inventory in all nearby DCs**<br>2. Customers can order multiple items at the same time | 1. Availability requests should be fast — **`< 100 ms`** — to support use cases like search<br>2. Ordering should be **strongly consistent**: two customers cannot purchase the same physical product<br>3. Support **10k DCs and 100k items** in the catalogue across DCs<br>4. Order volume will be **O(10M orders/day)** |

**Below the line** — Handling payments and purchases · driver routing and deliveries · search functionality and catalogue APIs · cancellations and returns · *NFR below the line:* privacy and security · disaster recovery

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Availability `< 100 ms` is a *union over nearby DCs* — cannot be a join at request time<br>- Ordering must be strongly consistent: two carts, one physical unit<br>- Two opposite guarantees over the same inventory rows<br>- (staff: which DC can reach a location in one hour is itself an expensive computation) | - Stale-fast availability cache vs live inventory — stale means occasional checkout failure; live means the read path pays for transactions<br>- `SELECT FOR UPDATE` vs conditional `UPDATE … WHERE qty >= n` — row lock is general; conditional write is lock-free and enough here<br>- Reserve-then-decrement vs decrement-on-order — reservations need TTL cleanup but stop overselling during checkout<br>- (staff: how wrong the availability projection is allowed to be is a product number, not an engineering one) | - Precompute `location → [nearby DC ids]` (travel-time isochrones) offline; availability = cache lookup over that DC set<br>- Inventory table per (DC, item) with `qty`; order = single transaction decrementing every line conditionally, rollback on any failure<br>- Read-your-own-writes for the cart via the primary<br>- (staff: availability is a projection allowed to be wrong; order path is the only truth; reservation separated from decrement; graceful checkout failure is part of the product) |

> The split personality is the point: a fast, stale-tolerant *availability* read path and a
> slow, transactional *order* write path over the same inventory. Two NFRs, two designs,
> one system. Same shape as [5. Ticketmaster](#5-ticketmaster) and
> [Flash Sale](#flash-sale-no-local-notes).

## 5. Ticketmaster

[notes](05-ticketmaster.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/ticketmaster)

**Crux** — The **hold with a TTL**, and surviving the on-sale moment when 10M people read one seat map.

| Functional | Non-functional |
|---|---|
| 1. Users can view events<br>2. Users can search for events<br>3. Users can book tickets to events | 1. **Availability for searching and viewing events, consistency for booking** — no double booking<br>2. Scalable to high throughput on popular events — **10 million users, one event**<br>3. Low latency search — **`< 500 ms`**<br>4. Read heavy — **100:1** — so it needs high read throughput |

**Below the line** — Viewing your booked events · admins or event coordinators adding events · dynamic pricing for popular events · *NFR below the line:* user-data protection and GDPR · fault tolerance · secure payment transactions · testing and CI/CD · regular backups

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - 10M users hitting one seat map at on-sale — the read path must not touch the DB<br>- No double booking — the seat needs a **hold with a TTL**, i.e. a distributed lock that expires<br>- Search `< 500 ms` across events while the booking path is hammered<br>- (staff: the booking path must see *bounded* traffic — the problem is admission, not throughput) | - Redis lock with TTL vs reservation row with expiry in the DB — Redis is fast and auto-expires; the DB row is the system of record and needs a sweeper<br>- Poll vs push (SSE/WS) for seat-map freshness — push is fresher; polling is simpler and cache-friendly<br>- Availability (browse) vs consistency (book) — stated per operation, not per system<br>- (staff: a waiting room trades fairness/UX for a bounded backend; who wins when lock and DB disagree) | - Events/venues/seats in Postgres; search via ES kept fresh async<br>- `POST /hold` → Redis `SET seat NX EX 600` (or reservation row `status=held, expires_at`) → `POST /book` idempotent on hold id → transaction to `booked`<br>- Seat map served from cache; invalidated on hold/book<br>- (staff: virtual waiting room issuing tokens = admission control; push seat-map updates; dynamic pricing and scalping as policy the model must permit) |

> This is the canonical "one CAP sentence per operation" problem. Also note what the
> booking requirement really demands: a **hold** with an expiry, which is a distributed
> lock with a TTL, which is where the entire deep dive lives.

## 6. Instagram

[notes](06-instagram.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/instagram)

**Crux** — **Fan-out on write versus read**, and the follower threshold where you switch.

| Functional | Non-functional |
|---|---|
| 1. Users can create posts featuring photos, videos and a simple caption<br>2. Users can follow other users<br>3. Users can see a **chronological** feed of posts from the users they follow | 1. Highly available, **availability over consistency** for photos and videos — eventual consistency is fine, **up to 2 minutes**<br>2. Feed content delivered with low latency — **`< 500 ms` end-to-end** for a feed request<br>3. Photos and videos **render instantly** (low-latency media delivery)<br>4. Scalable to **500M DAU** — with 100M posts/day<br>5. ° Follower counts are unbounded, so the **celebrity fan-out** case must not degrade everyone else<br>6. ° Read:write is roughly 100:1, so the feed is precomputed<br>7. ° Photos to 8 MB, videos to 4 GB — stated in a deep-dive heading, not in the requirements |

**Below the line** — Likes and comments · search over users, hashtags and locations · stories (ephemeral content) · going live (real-time video streaming) · °direct messages · °moderation · *NFR below the line:* security — authentication, authorization, encryption · fault tolerance and no data loss · analytics on user behaviour and engagement

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Feed `< 500 ms` for 500M DAU — a `JOIN follows → posts ORDER BY time` per request cannot hold<br>- Celebrity fan-out: one post × 100M followers = 100M inbox writes<br>- Media to 4 GB must render instantly — delivery is a CDN problem, not an API one<br>- (staff: the write-amplification number that sets the celebrity threshold is derivable — derive it) | - Fan-out on write (fast reads, write amplification, wasted work for inactive users) vs fan-out on read (cheap writes, slow reads) vs hybrid<br>- Bounded feed window vs full precompute — bounded caps storage; deep scroll needs a backfill<br>- Cursor vs offset pagination — the feed shifts under the reader; offsets duplicate/skip<br>- (staff: chronological now vs ranked later — design the store so the switch is not a rewrite) | - Posts, follows, feed tables; media uploaded direct to S3, transcoded into renditions, served via CDN<br>- Async fan-out workers push post ids into per-follower feed cache (Redis list, bounded to ~500)<br>- Celebrity (> N followers) posts merged in at read time; cursor pagination on `(created_at, post_id)`<br>- (staff: threshold from posts/day × avg followers; backfill on deep scroll; ranking-ready storage) |

> **Where the derived list differed.** The staleness budget is **2 minutes**, not 1 — the
> earlier row was tighter than HelloInterview asks for. **Celebrity fan-out** and the
> **100:1 read:write ratio** are not on its NFR list at all; they are yours, and they are
> still the right things to volunteer, because the hybrid fan-out trigger is what the
> deep dive turns on. The 8 MB photo / 4 GB video sizes appear in a deep-dive heading, not
> in the requirements. NFRs it puts *below* the line: security (authn, authz, encryption),
> fault tolerance and no data loss, and engagement analytics.

## 7. FB News Feed

[notes](07-fb-news-feed.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/fb-news-feed)

**Crux** — Same machine as Instagram, but **unbounded follower counts make hybrid fan-out mandatory**, not optional.

| Functional | Non-functional |
|---|---|
| 1. Users can create posts<br>2. Users can friend/follow people<br>3. Users can view a feed of posts from people they follow, in **reverse-chronological order**<br>4. Users can page through their feed | 1. Highly available (availability over consistency) — tolerate **up to 1 minute of post staleness**<br>2. Posting and viewing the feed return in **`< 500 ms`**<br>3. Handle a massive number of users — **2B**<br>4. Users can follow, and be followed by, an **unlimited** number of users |

**Below the line** — Likes and comments · private or restricted-visibility posts · °ranked (non-chronological) feed · °ads injection · °notifications

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - 2B users with **unbounded** follower counts — pure fan-out-on-write is impossible, not just expensive<br>- Feed `< 500 ms`, 1-minute staleness budget must be spent deliberately<br>- Feed store must survive unfollow / visibility changes without going stale forever<br>- (staff: the feed is a materialized view with a real cost per DAU — price it) | - Hybrid fan-out threshold — lower means more read-time merging; higher means more write amplification<br>- Queue-driven async fan-out with backpressure vs synchronous fan-out at post time<br>- Invalidate on unfollow vs filter at read — filter is cheaper, leaks briefly<br>- (staff: which parts survive a ranked feed — the fan-out machinery does; chronological ordering does not) | - Post service → Kafka → fan-out workers → per-user feed cache (Redis), partitioned by user id<br>- Users over the threshold are excluded from fan-out and merged at read from a per-author recent-posts cache<br>- Cursor paging; the 1-minute budget spent on batching fan-out<br>- (staff: rebuild path for the feed; explicit invalidation on unfollow and privacy change; ranking-ready) |

> Nearly identical to [6. Instagram](#6-instagram), with one difference that matters:
> "unlimited followers" is stated as a requirement, which makes hybrid fan-out mandatory
> rather than a nice deep dive. Paging is also explicit — **cursor**, not offset, because
> the feed shifts under the reader.

## 8. Tinder

[notes](08-tinder.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/tinder)

**Crux** — Two hard things hiding behind a simple product: the **ever-growing exclusion set**, and making a mutual swipe produce exactly one match.

| Functional | Non-functional |
|---|---|
| 1. Users can create a profile with preferences (age range, interests) and specify a **maximum distance**<br>2. Users can view a stack of potential matches in line with their preferences and within max distance of their current location<br>3. Users can swipe right/left on profiles **one by one**<br>4. Users get a **match notification** if they mutually swipe on each other | 1. **Strong consistency for swiping** — if a user swipes yes on someone who already swiped yes on them, they get a match notification<br>2. Scale to **20M daily actives, ~100 swipes/user/day** on average<br>3. Load the potential-matches stack with low latency — **`< 300 ms`**<br>4. **Avoid showing profiles the user has previously swiped on**<br>5. ° That works out to ~**2B swipes/day (~25k writes/s)**<br>6. ° The stack must be **pre-fetched** so the next card is instant<br>7. ° The per-user exclusion set grows **forever** |

**Below the line** — Uploading pictures · chatting via DM after matching · super swipes and other premium features · *NFR below the line:* protection against fake profiles · monitoring and alerting

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - The **seen set grows forever** — never re-show a swiped profile, checked on every card<br>- Two mutual swipes must produce exactly **one** match, under concurrent writes<br>- Candidate generation (geo + preferences) at 20M DAU × 100 swipes<br>- (staff: desirability skew — a fair-looking system shows everyone the same 1%) | - Bloom filter for seen-set (compact, false positives hide real people) vs precomputed stack (exact, stale as the user moves)<br>- Conditional write on a canonical pair key vs transaction for match creation — the pair key is lock-free<br>- Precompute the stack vs compute on demand — precompute is fast but goes stale with location<br>- (staff: whether Bloom false positives are acceptable is a product call; state the fallback) | - Profiles + preferences; swipes table keyed `(swiper, swipee)`; matches keyed on `min(a,b):max(a,b)`<br>- Swipe write checks the reciprocal atomically (conditional put / transaction) → creates match once<br>- Candidate stack precomputed per user, cached, filtered against seen set (Bloom or stack)<br>- (staff: stack refresh on movement; exposure allocation so top profiles do not starve the rest) |

> "Never re-show a swiped profile" looks like a product nicety and is actually the hardest
> requirement on the page: it is a growing per-user exclusion set intersected with a geo
> query on every stack load. Bloom filter, or precomputed stacks — say which and why.

## 9. LeetCode

[notes](09-leetcode.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/leetcode)

**Crux** — **Running hostile code safely**, and absorbing a competition spike onto a system that is otherwise tiny.

| Functional | Non-functional |
|---|---|
| 1. Users can view a list of coding problems<br>2. Users can view a given problem and code a solution in **multiple languages**<br>3. Users can submit their solution and get **instant feedback**<br>4. Users can view a **live leaderboard** for competitions | 1. Prioritize **availability over consistency**<br>2. **Isolation and security when running user code**<br>3. Return submission results within **5 seconds**<br>4. Scale to competitions with **100,000 users**<br>5. ° HI states in prose that the corpus is tiny — a few hundred thousand users, ~4k problems — so this is *not* a storage problem<br>6. ° Competition submissions arrive as a **spike**, not a stream |

**Below the line** — User authentication · user profiles · payment processing · user analytics · social features · *NFR below the line:* fault tolerance · secure purchase transactions · testing and CI/CD · regular backups

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Running **hostile code** with a result `< 5 s` — isolation and cold start are the same budget<br>- A 100k-user competition spike onto a system that is otherwise tiny<br>- Leaderboard updated in near real time during the competition<br>- (staff: container cold start is where the 5 s actually goes) | - Containers + seccomp vs gVisor vs microVMs (Firecracker) — stronger isolation, slower start, more cost<br>- Pre-warmed per-language pools vs on-demand — pre-warm buys latency, costs idle capacity<br>- Sync execution vs queue + poll — queue survives the spike and lets you degrade honestly<br>- (staff: time out vs show a queued state — the second is the honest degradation) | - `POST /submissions` → queue → worker pool of ephemeral sandboxes (no network, CPU/mem/time limits) → test runner → result store<br>- Autoscale workers on queue depth; pre-provision for a known competition<br>- Leaderboard in a Redis sorted set keyed by competition<br>- (staff: pre-warmed language images; tenant isolation boundaries; sandbox-escape blast radius) |

> The scale line is the trap: it is small everywhere except the competition spike and the
> sandbox. Spending the hour sharding a problems table is how this one is failed.

## 10. WhatsApp

[notes](10-whatsapp.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/whatsapp)

**Crux** — **Delivery guarantees to a client that is usually offline**, with retention as a deletion requirement.

| Functional | Non-functional |
|---|---|
| 1. Users can start **group chats** with multiple participants (**limit 100**)<br>2. Users can send and receive messages<br>3. Users can receive messages sent while they are **not online (up to 30 days)**<br>4. Users can send and receive **media** in their messages | 1. Messages delivered to available users with low latency — **`< 500 ms`**<br>2. **Guarantee deliverability** — messages should make their way to users<br>3. Handle **billions of users** with high throughput<br>4. Messages stored on centralized servers **no longer than necessary**<br>5. **Resilient against failures of individual components**<br>6. ° Ordering within a chat is preserved<br>7. ° ~40k messages/s fanning out to **~100k writes/s** through group chats |

**Below the line** — Audio and video calling · interactions with businesses · registration and profile management · *NFR below the line:* exhaustive treatment of security concerns (°including a full end-to-end-encryption design) · spam and scraping prevention

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Deliver to a client that is **offline for up to 30 days** — messages must be stored, then deleted once delivered<br>- Ordering within a chat under retries and reconnects<br>- Group sends fan out to up to 100 inboxes atomically enough<br>- (staff: a reconnect stampede after a connection-tier outage) | - Per-user inbox queue vs per-chat log — inbox makes delivery and deletion simple; per-chat makes history simple<br>- Delete on delivery (privacy, cheap storage) vs retain (search, multi-device)<br>- Sent / delivered / read as three acks vs one — three costs more messages, gives the product<br>- (staff: end-to-end encryption removes server-side search, media dedupe and server group fan-out — say whether that trade is being made) | - Chat service holds WebSockets; session registry maps user → connection server<br>- Message written to per-recipient inbox (Cassandra/Dynamo, TTL 30 d); pushed if online; drained and deleted on ack<br>- Per-chat monotonic sequence numbers for ordering and gap detection; media uploaded to blob and sent by reference<br>- (staff: connection tier as its own sharded service; three independent acks; E2E stated) |

> "Stored no longer than necessary" is the unusual one — a *deletion* requirement, which
> inverts the normal storage conversation and rules out treating the message store as an
> append-only log of record.

## 11. Strava

[notes](11-strava.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/strava)

**Crux** — **Offline-first**: the phone is the source of truth and the server is a sync target.

| Functional | Non-functional |
|---|---|
| 1. Users can **start, pause, stop and save** their runs and rides<br>2. While running or cycling, users can view activity data including **route, distance and time**<br>3. Users can view details about their own completed activities as well as the activities of their friends | 1. Highly available — **availability >> consistency**<br>2. **The app should function in remote areas without network connectivity**<br>3. Provide the athlete with **accurate and up-to-date local statistics** during the run/ride<br>4. Scale to **10 million concurrent activities**<br>5. ° Local stats are computed **on device**; the phone is the source of truth until it can sync |

**Below the line** — Adding or deleting friends (friend management) · authentication and authorization · commenting or liking runs · °segments and segment leaderboards · °route planning · *NFR below the line:* GDPR and data-privacy compliance · advanced security measures

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Must work with **no network** — recording, stats and display cannot depend on the server<br>- Same activity uploaded twice (retry, two devices) must not create two activities<br>- 10M concurrent activities streaming GPS points<br>- (staff: GPS drift, tunnels, clock skew — input is dirty by nature) | - Server as source of truth vs phone as source of truth — the NFR forces the phone<br>- Stream points live vs batch-upload on finish — live enables sharing; batch is simpler and battery-friendly<br>- Compute stats on device vs on server — on device means display never waits<br>- (staff: battery and mobile data as first-class constraints on the sync protocol) | - App buffers GPS locally (SQLite), computes stats on device, uploads in idempotent batches keyed by `activity_id` generated on the phone<br>- Server stores activities as eventually consistent; feed built from friends' activities<br>- Retry until ack; server dedupes on activity id<br>- (staff: dedupe across devices; smoothing/outlier handling; upload only on Wi-Fi/charging policy) |

> The only problem in the set whose primary requirement is *offline-first*. The server is
> a sync target, not a live participant — say that in minute three and the rest follows.

## 12. Distributed Cache

[notes](12-distributed-cache.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/distributed-cache)

**Crux** — Consistent hashing and **hot keys**. The LRU everyone rushes to is the easy half.

| Functional | Non-functional |
|---|---|
| 1. Clients can **set, get and delete** key-value pairs<br>2. Clients can configure the **expiration time (TTL)** for key-value pairs<br>3. Data is evicted according to an **LRU** policy — HI flags this as a choice: ask whether they want LFU, FIFO or custom | 1. Highly available; **eventual consistency is acceptable**<br>2. Low latency operations — **`< 10 ms` for get and set**<br>3. Scalable to **1 TB of data and 100k requests/s**<br>4. ° Adding or losing a node moves a minimal share of keys — consistent hashing, not modulo<br>5. ° A cache miss is correct behaviour, never an error |

**Below the line** — Configuring the cache size · °cache-aside vs write-through policy in the client · *NFR below the line:* durability and persistence across restarts · strong consistency guarantees · complex querying capabilities · transaction support

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - `< 10 ms` get/set at 100k RPS over 1 TB — more than one node, so keys must be sharded<br>- Adding/removing a node must not re-map every key<br>- A **hot key** can exceed one node's capacity regardless of sharding<br>- (staff: expiry and rebalance both cause a stampede onto the origin) | - Modulo vs consistent hashing — modulo remaps everything on a node change; consistent hashing moves 1/N<br>- LRU vs LFU vs TTL-only eviction — LRU is the default; LFU protects against scans<br>- Replicate for read availability vs accept a miss as correct — replication costs memory; a miss is not an error<br>- (staff: build vs run Redis Cluster — and what breaks first at 1 TB) | - Per node: hash map + intrusive doubly-linked list for O(1) LRU; TTLs; memory accounting including overhead<br>- Consistent hash ring with virtual nodes; client-side or proxy routing<br>- Replication for reads; writes explicitly non-durable<br>- (staff: hot-key replication or client-local short-TTL copy; single-flight and jittered TTLs against stampede) |

> This is the one problem where the *data structure* answer is the right one — a hash map
> plus an intrusive doubly-linked list per node — and the system work is all in
> partitioning, replication and hot-key handling. Do not let it become a Redis feature tour.

## 13. Rate Limiter

[notes](13-rate-limiter.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/distributed-rate-limiter)

**Crux** — **Where the counter lives** given a 10 ms budget, and what the limiter does when it is itself unavailable.

| Functional | Non-functional |
|---|---|
| 1. The system identifies clients by **user ID, IP address or API key** to apply appropriate limits<br>2. It limits HTTP requests based on **configurable rules** (e.g. 100 API requests per minute per user)<br>3. When limits are exceeded, requests are rejected with **HTTP 429** plus helpful headers — rate limit remaining, reset time | 1. **Minimal latency overhead — `< 10 ms` per request check**<br>2. Highly available; eventual consistency is ok, since slight delays in limit enforcement across nodes are acceptable<br>3. Handle **1M requests/second across 100M DAU**<br>4. ° Fails **open** — if the limiter is down, traffic passes rather than the site going down |

**Below the line** — Complex querying or analytics on rate-limit data · long-term persistence of rate-limiting data · °billing and quota enforcement · °per-endpoint cost weighting · *NFR below the line:* strong consistency guarantees across all nodes

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - `< 10 ms` added latency on **every** request at 1M req/s — one central Redis is the bottleneck<br>- Counter update must be atomic across concurrent requests<br>- What happens when the limiter itself is down<br>- (staff: rules and multi-tier limits must propagate to every node consistently) | - Token bucket vs sliding-window log vs sliding-window counter — log is exact but memory-heavy; counter approximates cheaply<br>- Central Redis (exact, single hop) vs local-first with async global reconciliation (fast, slightly over-admits)<br>- Fail open vs fail closed — open protects availability, opens an abuse window<br>- (staff: where the check lives — gateway vs per-service — is a blast-radius call) | - Check at the API gateway; keyed by user/API key/IP; 429 with `Retry-After`, `X-RateLimit-*` headers<br>- Redis with Lua script (or `INCR` + `EXPIRE`) for atomic sliding-window counter<br>- Rules in config, cached locally, pushed on change<br>- (staff: per-cell quota slices with async reconciliation; stated fail-open policy; who rate-limits the rate limiter) |

> Two requirements do the work: `< 10 ms` (so the counter lives in memory near the edge,
> not in your database) and "eventual consistency is ok" (so you may allow a little bleed
> rather than coordinating). Choosing the algorithm — token bucket vs sliding window log vs
> sliding window counter — is downstream of both.

## 14. Online Auction

[notes](14-online-auction.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/online-auction)

**Crux** — **Serializing bids on one row** while broadcasting the new price to millions, in the ten seconds where everything arrives at once.

| Functional | Non-functional |
|---|---|
| 1. Users can post an item for auction with a starting price and an end date<br>2. Users can bid on an item; a bid is accepted only if it is higher than the current highest bid<br>3. Users can view an auction, including the current highest bid | 1. **Strong consistency for bids** — every user sees the same highest bid<br>2. **Fault tolerant and durable: no bid is ever dropped**<br>3. The current highest bid is displayed **in real time**, so bidders know what they are bidding against<br>4. Scale to **10M concurrent auctions**<br>5. ° A bid is acknowledged accepted or rejected in `< 500 ms`<br>6. ° A new highest bid reaches all watchers in `< 1 s`, or people bid against stale prices<br>7. ° The close is exact: a bid one millisecond after the end time loses<br>8. ° The sniping spike — ~10k bids/s on one item in the final seconds — is yours to raise; it is **not** HI's stated scale |

**Below the line** — Search for items · filter items by category · sort items by price · view the auction history of an item · °payments and escrow · °shill-bidding and fraud detection · °proxy/automatic bidding · *NFR below the line:* observability and monitoring · security and user-data protection · testing and deployment (CI/CD)

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - All bids for a hot auction land on **one row** in the last ten seconds<br>- No bid may be dropped; the highest bid must be strongly consistent<br>- Broadcast the new price to millions of watchers<br>- (staff: money — disputes are settled from the log, so the log must be complete) | - Row lock vs compare-and-set on `current_max` — CAS avoids the lock but needs a retry loop<br>- Single-writer partition per auction (ordering free, no contention) vs shared DB row (simpler, contended)<br>- SSE vs WebSocket vs polling for price — SSE is enough (one direction)<br>- (staff: anti-snipe extension is policy the model must support, not decide) | - Append-only bids table + `auctions.current_max` maintained by atomic CAS `UPDATE … WHERE current_max < :bid`<br>- Close time authoritative on the server; late bids rejected<br>- Price pushed via SSE per auction; outbid notifications via outbox<br>- (staff: route each auction's bids to one partition/actor; outbox so broadcast never loses or duplicates) |

> **Where the derived list differed.** The published scale number is **10M concurrent
> auctions** — a breadth figure. The earlier row's *"10k bids/s on one hot item, 10M
> watchers"* is not HelloInterview's; it was inferred. Keep the sniping spike as the thing
> you raise yourself — contention on **one row** is still what separates this from
> [5. Ticketmaster](#5-ticketmaster) — but do not present it as the stated scale. Note that
> "durable, we can't drop any bids" is published, so the audit-log argument is on-scope.
> NFRs it puts *below* the line: observability and monitoring, security, testing and CI/CD.

## 17. YouTube

[notes](17-youtube.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/youtube)

**Crux** — The **transcode pipeline and segmented adaptive delivery**. Upload and playback are both consequences of it.

| Functional | Non-functional |
|---|---|
| 1. Users can **upload** videos<br>2. Users can **watch (stream)** videos | 1. Highly available — availability over consistency<br>2. Support uploading and streaming **large videos (10s of GBs)**<br>3. **Low-latency streaming even in low-bandwidth environments**<br>4. Scale to **~1M videos uploaded/day, 100M videos watched/day**<br>5. Support **resumable uploads**<br>6. ° Requirement 3 is what makes **adaptive bitrate** a requirement rather than an optimization |

**Below the line** — Viewing information about a video, such as view counts · searching for videos · commenting on videos · recommended videos · creating and managing a channel · subscribing to channels · *NFR below the line:* protection against bad content · protection against bots and fake accounts · monitoring and alerting

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Upload of 10s of GB must be resumable and then **transcoded into a ladder of renditions**<br>- Playback on poor bandwidth — the client must switch quality mid-stream<br>- 100M watches/day — delivery is CDN, not origin<br>- (staff: transcoding a 10 GB file per-file takes hours; per-segment takes minutes) | - HLS vs DASH — both segment + manifest; pick one and move on<br>- Transcode per file vs per segment — per segment parallelises and lets playback start early<br>- Eager vs lazy renditions — eager costs storage for the long tail nobody watches<br>- (staff: store tiers and lazy transcode because ~1% of videos take ~99% of watch time) | - Multipart resumable upload to blob storage; metadata row `status=uploading`<br>- Transcode job graph (DAG) → renditions → HLS/DASH segments + manifest → CDN; thumbnails on completion<br>- Player fetches manifest, adapts bitrate per segment<br>- (staff: segment-level parallel transcode; queue prioritised by expected audience; cold tier for the tail) |

> Two functional requirements, and both are hard. "Low latency in low bandwidth" plus
> "10s of GB" together mandate the transcode-into-a-ladder-of-renditions pipeline and
> segment-level CDN delivery — everything else in the hour hangs off that.

## 18. Job Scheduler

[notes](18-job-scheduler.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/job-scheduler)

**Crux** — Turning *fire at time T, exactly once* into **durable timers plus idempotent at-least-once execution**.

| Functional | Non-functional |
|---|---|
| 1. Users can schedule jobs to be executed **immediately, at a future date, or on a recurring schedule** ("every day at 10:00 AM")<br>2. Users can monitor the status of their jobs | 1. Highly available — **availability > consistency**<br>2. Jobs execute **within 2 s of their scheduled time**<br>3. Scalable to **10k jobs per second**<br>4. **At-least-once execution** of jobs<br>5. ° At-least-once is what licenses **idempotency keys** — make duplicate runs safe, and say why exactly-once across a process boundary does not exist<br>6. ° Automatic **retry with backoff**, dead-lettering after N attempts<br>7. ° An accepted job is durable — never lost, even if the scheduler crashes between accept and fire<br>8. ° Scheduling API responds `< 50 ms`; the fire path is decoupled from it |

**Below the line** — Cancelling or rescheduling jobs · °DAG dependencies between jobs · °the execution sandbox and resource manager · °job output storage · °per-tenant quotas · *NFR below the line:* enforcing security policies · CI/CD

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Fire **within 2 s** of due time at 10k jobs/s — a full-table scan per second cannot<br>- At-least-once execution, but retries must not double-run side effects<br>- A dead worker's job must be retried; a restarted scheduler must not double-fire<br>- (staff: catch-up after an outage — skip or backfill — changes the semantics) | - DB poller vs in-memory timer wheel — DB is durable and slow; memory is precise and volatile; split by horizon<br>- At-least-once + idempotency vs exactly-once claims — only the first is real<br>- Leases/visibility timeouts vs explicit locks — leases self-heal on worker death<br>- (staff: clock skew — trust the scheduler's clock, not the worker's) | - Jobs table with `next_run_at`, time-bucketed index; scheduler pulls due jobs into a queue (SQS/Kafka)<br>- Workers take a lease, execute, ack; retries with backoff; DLQ; dedupe key = `(job_id, fire_time)`<br>- Recurring jobs re-enqueued after run with the next `next_run_at`<br>- (staff: far-future in DB, imminent in a timer wheel; partitioned ownership with leader election; explicit catch-up policy) |

> **Where the derived list differed.** The tolerance is **2 seconds**, not ~1 — the earlier
> row was stricter than the question asks. **At-least-once is HelloInterview's own wording**,
> so the idempotency-key elaboration is exactly the right move: it converts the impossible
> "exactly once" conversation into the tractable one. Retry with backoff, dead-lettering and
> the `< 50 ms` scheduling API were inferred — good deep-dive material, not stated
> requirements. Compare [36. Durable Execution](#36-durable-execution-engine), which makes
> the same trade explicitly. NFRs it puts *below* the line: security policies, CI/CD.

## 19. FB Live Comments

[notes](19-fb-live-comments.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/fb-live-comments)

**Crux** — **One write fanning out to a million sockets**, plus a backlog for viewers who arrived late.

| Functional | Non-functional |
|---|---|
| 1. Viewers can **post comments** on a live video feed<br>2. Viewers can see **new comments being posted** while they are watching<br>3. Viewers can see **comments made before they joined** the live feed | 1. Scale to **millions of concurrent videos and thousands of comments per second per live video**<br>2. Prioritize **availability over consistency**; eventual consistency is fine<br>3. Low latency — broadcast comments in near-real time, **`< 200 ms` end-to-end** under typical network conditions<br>4. ° A viewer joining mid-stream gets recent history without a thundering-herd read |

**Below the line** — Replying to comments · reacting to comments · °comment ranking · *NFR below the line:* security — only authorized users can post · integrity constraints on comment appropriateness (spam, hate speech)

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - One comment must reach up to a million open sockets in `< 200 ms`<br>- Late joiners need the backlog without the live path paying for it<br>- Thousands of comments/s on one hot video is more than any client can render<br>- (staff: every connection server subscribing to every video does not scale — a dispatcher tree does) | - WebSocket vs SSE — comments are one-way to viewers; SSE is enough and cheaper<br>- Pub/sub per video vs polling — polling at this scale is a DB attack<br>- Buffer slow consumers vs drop them — buffering leaks memory; dropping is honest<br>- (staff: sampling/ranking comments is a requirement at hot scale, not a compromise) | - Comment write → DB + pub/sub topic per video<br>- Connection servers hold sockets with a subscription registry; subscribe to the video's channel; batch/coalesce on hot videos<br>- On join: paginate backlog from storage, then attach to live stream<br>- (staff: dispatcher tree; drop slow consumers; cost per open connection) |

> Requirement 3 (history) is what makes this more than a pub/sub demo: you need a
> **persisted, paginated backlog** alongside the live fan-out, and one write has to feed
> both. The interesting number is the fan-out ratio — one comment to millions of sockets.

## 20. News Aggregator

[notes](20-news-aggregator.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/google-news)

**Crux** — An ingest pipeline over **sources you do not control**, feeding a read path that must survive a breaking-news spike.

| Functional | Non-functional |
|---|---|
| 1. Users can view an aggregated feed of news articles from **thousands of source publishers** all over the world<br>2. Users can scroll through the feed "infinitely"<br>3. Users can click an article and be redirected to the publisher's website for the full content | 1. **Availability over consistency** (CAP) — slightly outdated content beats no content<br>2. Scalable to **100M DAU, with spikes up to 500M**<br>3. Low-latency feed load times — **`< 200 ms`**<br>4. ° A newly published article appears in the feed within **minutes**, not seconds<br>5. ° Ingest is a pull pipeline over sources you do not control: it must tolerate slow, broken and rate-limiting publishers without stalling |

**Below the line** — Customising the feed based on interests · saving articles for later reading · sharing articles on social media · °hosting full article content (a licensing problem, not a technical one) · °comments · *NFR below the line:* protecting user data and privacy · **handling traffic spikes during breaking-news events** · monitoring and observability · resilience against publisher API failures

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Ingest from **publishers you do not control** — one slow source must not stall the pipeline<br>- Feed `< 200 ms` at 100M DAU with spikes to 500M on breaking news<br>- The same story appears on 50 publishers — dedupe/cluster or the feed is garbage<br>- (staff: the spike concentrates on the same few articles — it is an edge-caching problem) | - Poll publishers on a cadence vs RSS/push — poll is universal; push is fresher where available<br>- Precomputed feed pages vs personalised on read — precompute holds the spike; personalisation needs a merge<br>- Full content vs snippet + redirect — licensing decides this, not engineering<br>- (staff: clustering quality vs cost — it is the product surface) | - Per-publisher fetchers with own cadence and circuit breaker → parse → dedupe (URL + content hash) → cluster → article store<br>- Feed pages precomputed per topic/region behind cache + CDN; infinite scroll by cursor<br>- AP everywhere: stale news beats no news<br>- (staff: request coalescing at the edge; snippets and redirects for licensing; failure isolation per source) |

> **Where the derived list differed — two real corrections.** First, the feed budget is
> **`< 200 ms`**, not 500 ms; the earlier row was 2.5× loose on the one number that sizes
> the read path. Second, HelloInterview puts **breaking-news traffic spikes *below* the
> line**, together with resilience against publisher API failures, privacy and
> observability — the earlier note called the spike "the design case". Treating it as the
> design case is still defensible and makes for a better deep dive, but say that you are
> promoting it, rather than implying the question handed it to you.

## 21. Price Tracking Service

[notes](21-price-tracking-service.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/camelcamelcamel)

**Crux** — **Spending a finite crawl budget well** against someone else's site, and storing 500M price series cheaply.

| Functional | Non-functional |
|---|---|
| 1. Users can view **price history** for Amazon products, via the website or a **Chrome extension** (1M active users, one-click subscribe from the product page)<br>2. Users can **subscribe to price-drop notifications with thresholds** | 1. Prioritize **availability over consistency** — eventual consistency acceptable<br>2. Handle **500 million Amazon products** at scale<br>3. Price history queries with **`< 500 ms`** latency<br>4. Deliver price-drop notifications **within 1 hour** of a price change<br>5. ° HI frames the system as needing to be **"polite" to Amazon** — crawl budget is finite, so poll popular products often and the long tail rarely |

**Below the line** — Search and product discovery on the platform · price comparison across multiple retailers · product reviews and ratings integration · *NFR below the line:* strong consistency for price data · real-time (sub-minute) price updates

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Detect a price change within 1 h across 500M products **on someone else's API/site budget**<br>- 500M price series stored cheaply<br>- One price drop can fan out into millions of alerts at once<br>- (staff: anti-bot and ToS — the crawl budget is not just rate limits) | - Uniform polling vs tiered by popularity — uniform is fair and impossible; tiered is the only feasible allocation<br>- Store every sample vs change points only — change points cut storage ~100×<br>- Alert per user per change (accurate, stormy) vs batched/deduped alerts<br>- (staff: the browser extension as a data source vs pure crawling) | - Product catalogue with popularity tier → scheduler emits fetch jobs → scrapers under per-host rate limits → price history (change points)<br>- Threshold check on change → alert queue with dedupe key `(user, product, price_point)`<br>- Cold history tiered to object storage<br>- (staff: expected-value polling allocation; site-wide-sale alert storm plan) |

> The 1-hour alert SLO is generous on purpose — it means you can poll on a **tiered
> cadence** and batch notifications. Candidates who assume "real time" build a system ten
> times larger than the requirement. Closely related to
> [38. Metrics Aggregation](#38-metrics-aggregation-platform), which formalizes the same
> "someone else's API is the bottleneck" constraint.

## 22. YouTube Top K

[notes](22-youtube-top-k.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/top-k)

**Crux** — Reconciling **exact counting with 700k events/s and a tens-of-milliseconds read** by splitting aggregation from serving.

| Functional | Non-functional |
|---|---|
| 1. Clients can query the **top K videos for all time** (up to a max of **1k results**)<br>2. Clients can query **tumbling windows of 1 hour, 1 day and 1 month**, and all-time (max 1k results) | 1. Tolerate at most **1 minute** delay between when a view occurs and when it is tabulated<br>2. **Results must be precise — no approximation** (HI revisits approximation only in the deep dives)<br>3. Return results within **tens of milliseconds**<br>4. Handle a massive number of views/s, and support a massive number of videos — HI leaves both **explicitly TBD** and estimates them later<br>5. ° The notes fill the TBDs in: ~70B views/day ≈ **700k views/s**, ~1M new videos/day, ~64 GB of all-time counters |

**Below the line** — Arbitrary time periods · arbitrary starting/ending points (all queries look back from the current moment) · °per-region or per-user top-K · °tie-breaking policy

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - **Exact** counts (no sketch) at ~700k views/s — the raw stream cannot be written to any DB row-per-event<br>- Top-K read in tens of ms for four windows — cannot sort at query time<br>- Partition skew: one viral video takes a large share of the stream<br>- (staff: window rollover must not leave a gap where the hour bucket is half-built) | - Count-min sketch / heavy hitters (cheap, approximate) vs exact aggregation — exactness disqualifies the sketch as the source of truth<br>- Tumbling vs sliding windows — tumbling is buildable from minute buckets; sliding is precluded (no arbitrary ranges)<br>- Kafka + Flink vs specialised OLAP (Pinot/Druid/ClickHouse) — the latter buys flexibility you were told not to need<br>- (staff: sketch as candidate generator + exact count on the shortlist — where approximation still helps) | - View events → Kafka partitioned by `video_id` (salted for hot videos) → Flink pre-aggregates per video per **minute**<br>- Minute counts written to a store; hour/day/month/all-time buckets maintained by rolling minute counts in and out<br>- Top-K per window materialised every minute into a sorted structure/cache; `GET /top-k?window=` is a cache read<br>- (staff: replay from Kafka after failure without double counting via checkpoint + idempotent upsert; rollover with double-buffered windows) |

> "Precise, not approximate" plus "700k views/s" is the tension the whole problem is built
> around. Write both on the board adjacently — the interviewer is watching for whether you
> notice they conflict.

## 23. Uber

[notes](23-uber.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/uber)

**Crux** — Matching as a **serialised decision over a geospatial index that is rewritten every few seconds**.

| Functional | Non-functional |
|---|---|
| 1. Riders can input a start location and a destination and get a **fare estimate**<br>2. Riders can request a ride based on the estimated fare<br>3. Upon request, riders are **matched with a driver** who is nearby and available<br>4. Drivers can accept/decline a request and navigate to pickup/drop-off | 1. **Low latency matching — `< 1 minute` to match or failure**<br>2. **Strong consistency in ride matching** — no driver is assigned multiple rides simultaneously<br>3. High throughput, especially during peak hours or special events — **100k requests from the same location**<br>4. ° Driver location updates stream continuously (~every 5 s per active driver): a write-heavy firehose whose freshness requirement is seconds and whose durability requirement is **none** |

**Below the line** — Riders rating their ride and driver post-trip · drivers rating passengers · scheduling rides in advance · different ride categories (X, XL, Comfort) · *NFR below the line:* security, privacy and GDPR compliance · resilience with redundancy and failover · monitoring, logging and alerting · CI/CD and low-downtime maintenance

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Driver locations update every few seconds — the geo index is **rewritten constantly**<br>- One driver, one ride: the accept race under 100k requests from one location<br>- Nearby-driver query must be fast while the index churns<br>- (staff: greedy nearest-driver is a worse outcome than batched assignment) | - Durable driver locations vs in-memory non-durable — durability buys nothing for data that is stale in 5 s<br>- Distributed lock on driver vs single-writer per driver — single writer makes the race disappear<br>- Greedy match per request vs batched regional matching — batched improves outcomes and changes the architecture<br>- (staff: surge as feedback control on supply, not a pricing table) | - Drivers push location → Redis geo index (or in-memory quadtree) per region, non-durable<br>- Ride request → query nearby drivers → offer to one with a timeout → on decline/timeout, next driver<br>- Matching guarded so a driver holds at most one offer (conditional write / driver actor)<br>- (staff: batch matching every few seconds solving an assignment; region shards with boundary handling) |

> The unstated requirement candidates miss: **driver location writes dwarf ride requests**
> and do not need to be durable. Saying "the last known location is disposable state" is
> what lets you keep it in memory and stop worrying about the write volume.

## 24. Robinhood

[notes](24-robinhood.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/robinhood)

**Crux** — **Two systems sharing a login**: a lossy price fan-out and a lossless order ledger.

| Functional | Non-functional |
|---|---|
| 1. Users can see **live prices** of stocks<br>2. Users can manage orders for stocks — market and limit orders, create and cancel | 1. **High consistency for order management** — it is essential that users see up-to-date order information when making trades<br>2. Scale to a high number of trades per day: **20M DAU × ~5 trades/day, 1000s of symbols**<br>3. Low latency reflecting symbol price updates and placing orders — **`< 200 ms`**<br>4. **Minimise the number of active clients connecting to an external exchange API** — exchange data feeds and client connections are expensive<br>5. ° Orders are durable and exactly-once: an acknowledged order is never lost or double-submitted, and a cancel either wins or reports that it lost<br>6. ° Millions of concurrent price subscriptions, heavily skewed to a few hundred symbols<br>7. ° **Graceful degradation** when the exchange feed lags — show staleness, never invent a price |

**Below the line** — Trading outside market hours · ETFs, options and crypto · seeing the order book in real time · °portfolio analytics · °clearing and settlement · °KYC · *NFR below the line:* connecting to multiple exchanges · trading fees and calculations · daily limits on trading behaviour · protection against bot usage

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Fan out prices for 1000s of symbols to 20M users off a **costly, single** exchange feed<br>- Orders are lossless and CP; prices are lossy and AP — in one app<br>- Market-open spike is predictable and brutal<br>- (staff: what the app shows when the exchange feed lags is a compliance question) | - Every tick vs conflated ticks per symbol — every tick cannot be delivered; conflation drops intermediate prices by design<br>- Orders through a durable log + state machine vs direct DB writes — the log makes replay and audit possible<br>- Hold limit orders in your system vs at the broker — decide and say where<br>- (staff: stale-price trading policy — badge it, halt it — handed to compliance) | - Exchange feed → single ingest → conflation per symbol → pub/sub → WebSocket fan-out by subscription<br>- Order service: idempotency key, durable log, explicit state machine (`pending → submitted → filled/cancelled`), then broker<br>- Separate read models for portfolio and price<br>- (staff: pre-scaled for market open; audit trail and reconciliation with the clearing broker) |

> **Where the derived list differed.** *"10k orders/s at the open"* is not
> HelloInterview's number — its scale is **20M DAU × 5 trades/day over 1000s of symbols**,
> which is a far smaller order rate and changes what you provision for. More important,
> the published list has a **fourth requirement the derived one missed entirely: the
> exchange feed is a cost constraint.** That is why you subscribe from a small pool of
> connections and fan out internally, rather than opening a feed per client — and it is
> the requirement that makes this a design rather than two CRUD services. The
> fan-out-vs-ledger split is still the right framing to lead with. NFRs it puts *below*
> the line: connecting to multiple exchanges, trading fees, daily trading limits, bot
> protection.

## 25. Google Docs

[notes](25-google-docs.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/google-docs)

**Crux** — **Convergence**: OT or CRDT, with the server as a sequencer rather than a lock manager.

| Functional | Non-functional |
|---|---|
| 1. Users can create new documents<br>2. **Multiple users can edit the same document concurrently**<br>3. Users can view each other's changes in real time<br>4. Users can see the cursor position and presence of other users | 1. Documents should be **eventually consistent** — all users eventually see the same document state<br>2. Updates should be low latency — **`< 100 ms`**<br>3. Scale to **millions of concurrent users across billions of documents**<br>4. **No more than 100 concurrent editors per document**<br>5. Documents are **durable and available** even if the server restarts<br>6. ° Local edits echo **instantly** — optimistic, applied before the server replies<br>7. ° Presence and cursors are **lossy and cheap**: a weaker channel than edits |

**Below the line** — Sophisticated document structure (assume a simple text editor) · permissions and collaboration levels · document history and versioning · °offline editing and reconciliation · °comments and suggestion mode

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - 100 concurrent editors must **converge** on the same document without locking<br>- Local echo must be instant — the edit cannot wait for the server<br>- Document load time must stay bounded as the op log grows<br>- (staff: the owning process for a doc must fail over without losing ordering) | - OT (needs one sequencer per doc, compact) vs CRDT (no sequencer, metadata and tombstone growth)<br>- Server as sequencer vs server as lock manager — locks kill concurrency<br>- Snapshot frequency vs log size — more snapshots cost storage, cut load time<br>- (staff: what undo means once ops are transformed is a product+algorithm decision) | - WebSocket per editor; ops applied locally, sent with a version, transformed server-side against concurrent ops, broadcast<br>- Op log per document, compacted into periodic snapshots; presence on a separate lossy channel<br>- One owning process per document (consistent hash of doc id)<br>- (staff: failover by replaying the log onto a new owner; undo semantics defined) |

> **Where the derived list differed.** HelloInterview's word is **"eventually
> consistent"**. Your *"convergence, not consistency"* is still the better sentence to say
> out loud — the server is a sequencer, not a lock manager — just know you are sharpening
> its phrasing, not quoting it. The published list also frames the **100-editor cap as a
> gift**: it removes single-document throughput as a problem entirely, which is why the
> deep dive is about OT/CRDT correctness and not about sharding a hot document. (Real
> Google Docs does the same — past a threshold, new arrivals join as readers.) Offline
> editing, comments and suggestion mode were local additions, not out-of-scope items it
> lists.

## 26. Web Crawler

[notes](26-web-crawler.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/web-crawler)

**Crux** — Politeness caps **per-host** throughput, so aggregate throughput comes only from breadth — and the crawl must resume.

| Functional | Non-functional |
|---|---|
| 1. Crawl the web starting from a given set of **seed URLs**<br>2. **Extract text data** from each web page and store the text for later processing | 1. **Fault tolerance** — handle failures gracefully and resume crawling without losing progress<br>2. **Politeness** — adhere to `robots.txt` and do not overload website servers<br>3. **Efficiency** — crawl the web in **under 5 days**<br>4. **Scalability** — handle **10B pages** (~2 MB/page transfer including inline resources; HTML alone is ~30 KB)<br>5. ° Duplicate detection on both URL **and content** — the web is full of mirrors<br>6. ° Traps, redirect loops and infinite calendars must not starve the frontier |

**Below the line** — Processing the extracted text (e.g. training an LLM) · non-text data such as images and video · dynamic, JavaScript-rendered content · authenticated, login-required pages · *NFR below the line:* security against malicious actors · operating cost within budget · legal and privacy compliance

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - 25k pages/s aggregate, but **per-host politeness** caps each host — throughput comes only from breadth<br>- The crawl must resume after a crash without re-fetching 10B pages<br>- Dedupe URLs and content across mirrors; avoid traps<br>- (staff: 20 PB in 5 days is a bandwidth/cost problem, not CPU) | - Single frontier vs front (priority) + back (per-host politeness) queues — the split is the design<br>- Store raw HTML vs extracted text — raw is repairable, 10× the storage<br>- Bloom filter for seen URLs (compact, false positives skip pages) vs exact set<br>- (staff: recrawl policy by observed change rate vs fixed cadence) | - Frontier: priority queues → per-host queues with rate limiting; robots.txt and DNS cached<br>- Fetchers → parser → text to blob storage, links back to frontier; URL dedupe via Bloom/KV; content-hash dedupe<br>- Frontier and seen-set checkpointed durably for resumability<br>- (staff: per-domain budget against traps; bandwidth and egress costed) |

> "Politeness" and "25k pages/s" together are the design: you cannot crawl fast *per host*,
> so throughput comes only from **breadth**, which makes the frontier a per-domain queue
> partitioned across workers rather than one global queue.

## 27. Ad Click Aggregator

[notes](27-ad-click-aggregator.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/ad-click-aggregator)

**Crux** — **Lossless, idempotent ingest** feeding a fast approximate view and a slower authoritative one.

| Functional | Non-functional |
|---|---|
| 1. Users can **click on an ad and be redirected** to the advertiser's website<br>2. Advertisers can **query ad click metrics over time**, with a minimum granularity of **1 minute** | 1. Scalable to a peak of **10k clicks per second** — 10M active ads; average ~1k/s ≈ **100M clicks/day**<br>2. **Low latency analytics queries** for advertisers — sub-second response time<br>3. **Fault tolerant and accurate data collection — we should not lose any click data**<br>4. **As realtime as possible** — advertisers query data as soon as possible after the click<br>5. **Idempotent click tracking** — the same click is never counted multiple times |

**Below the line** — Ad targeting · ad serving · cross-device tracking · integration with offline marketing channels · *NFR below the line:* fraud and spam detection · demographic and geo profiling of users · conversion tracking

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Redirect must be fast and **must not block on the write**, yet **no click may be lost** — fire-and-forget loses data<br>- The same click must never count twice — retries, double-clicks, replays<br>- 10k clicks/s aggregated to 1-minute granularity and queryable sub-second<br>- (staff: this is billing data — stream results must be reconcilable with a batch recount) | - Kafka + Flink (near-real-time) vs batch-only (simple, hours late) vs both (lambda-style: authoritative batch, fast stream)<br>- Dedupe in Flink state (windowed, bounded memory) vs unique-constraint on the OLAP table (exact, slower)<br>- OLAP pre-aggregated table vs raw-event query — pre-agg is sub-second but only on fixed dimensions<br>- (staff: exactly-once via idempotent upserts keyed on click id vs trusting Flink's transactional sink) | - Click service stamps a `click_id`, redirects immediately, produces `{click_id, ad_id, campaign_id, user/device, ts}` to Kafka partitioned by `ad_id` (+ salt for hot ads)<br>- Flink: dedupe on `click_id` within a window, aggregate per `(ad_id, minute)`, watermarks for late events, sink to OLAP (ClickHouse/Redshift)<br>- Raw events archived to S3 from Kafka; hourly/daily batch recount reconciles the stream table<br>- `GET /metrics?ad_id&from&to&granularity` reads the pre-aggregated table<br>- (staff: replay from Kafka/S3 after a bad deploy without double counting; advertiser skew handled) |

> Requirements 3 and 5 together are why the click write path is a durable log with a
> dedupe key and not an increment. The redirect must also be fast and must not block on
> the write — state that, because it is the one latency budget on the ingest side.

## 28. FB Post Search

[notes](28-fb-post-search.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/fb-post-search)

**Crux** — An inverted index that must absorb **100k mutating like counts per second** without reindexing documents.

| Functional | Non-functional |
|---|---|
| 1. Users can **create and like posts**<br>2. Users can **search posts by keyword**<br>3. Users can get search results sorted by **recency or like count** | 1. Must be fast — **median queries return in `< 500 ms`**<br>2. Support a high volume of requests — HI estimates **10k posts/s, 100k likes/s, 10k searches/s** from 1B users, which makes this **write-heavy, not read-heavy**<br>3. New posts must be searchable in a short amount of time — **`< 1 minute`**<br>4. **All posts must be discoverable**, including old or unpopular ones — and we can take more time for those<br>5. Highly available<br>6. ° Ten years of posts ≈ **3.6T documents, ~3.6 PB raw** — so the index, not the query, is the problem |

**Below the line** — Fuzzy matching on terms ("bird" matches "ostrich") · personalization of search results · privacy rules and filters · sophisticated relevance algorithms for ranking · images and media · realtime updates to the search page as new posts arrive

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - 100k likes/s would mean **100k document reindexes/s** in a naive inverted index<br>- Every post searchable within 1 min; **all** posts discoverable — trillions of documents<br>- Two sort orders (recency, engagement) over the same index<br>- (staff: scatter-gather across thousands of shards has a tail-latency problem) | - Like count in the document (reindex per like) vs a separate score/doc-value merged periodically<br>- One index with two sorts vs two index organisations — two indexes cost storage, buy latency<br>- Hot recent shards vs cold archive — tiering is what makes 'all posts' affordable<br>- (staff: relevance ranking left out of scope, defended) | - Post create → Kafka → indexer → sharded inverted index (by term or by time)<br>- Likes → Kafka → periodic batch update of a like-score field, not document reindex<br>- Query: fan out to shards, merge; cursor pagination<br>- (staff: hot/cold shard tiers; tail-latency mitigation — hedged requests, shard sizing) |

> The two sort orders are the requirement that hurts: **recency is a static index order,
> like count is a mutating one**, and 100k likes/s means you cannot reindex a document on
> every like. Contrast [40. Document Search](#40-full-text-document-search), where the
> ranking signal is immutable.

## 29. Payment System

[notes](29-payment-system.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/payment-system)

**Crux** — **Idempotency, a state machine and reconciliation.** Money makes correctness the feature.

| Functional | Non-functional |
|---|---|
| 1. Merchants can initiate payment requests — charge a customer for a specific amount<br>2. Users can pay for products with credit and debit cards<br>3. Merchants can view status updates for payments: pending, success, failed | 1. The system should be **highly secure**<br>2. **Durability and auditability** — no transaction data is ever lost, even in case of failures<br>3. **Transaction safety and financial integrity despite the inherently asynchronous nature of external payment networks**<br>4. Scalable to high volume — **10,000+ TPS** — and bursty traffic (holiday sales)<br>5. ° A client-supplied **idempotency key** per request, so retries and double-submits never charge twice<br>6. ° Authorization completes `< 1 s` synchronously; capture and settlement are asynchronous<br>7. ° **PCI scope containment** — raw card data never touches your servers |

**Below the line** — Saved payment methods · refunds, full or partial · transaction history and reporting · alternative payment methods (bank transfers, digital wallets) · recurring payments and subscriptions · payouts to merchants · *NFR below the line:* adherence to global financial regulations · extensibility to new payment methods

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - The processor is **asynchronous and unreliable** — a timeout means the charge may or may not have happened<br>- Retries must provably never double-charge<br>- Money needs an audit answer to any dispute, forever<br>- (staff: your ledger and the processor's and the bank's will disagree — reconcile, do not assume) | - Status column vs double-entry append-only ledger — status cannot answer a dispute; the ledger can<br>- Sync call to processor vs async with webhook — async is the reality; design for it<br>- Own card data (PCI scope) vs hosted fields/tokenisation — buy this<br>- (staff: build vs buy — buy the processor, own the ledger) | - `POST /payments` with client idempotency key → payment intent row → call processor → state machine with legal transitions only<br>- Processor webhooks update state; outbox delivers merchant webhooks at-least-once<br>- On timeout: query processor by idempotency key before retrying<br>- (staff: double-entry ledger; three-way reconciliation ledger ↔ processor ↔ bank) |

> **Where the derived list differed.** HelloInterview does not say "exactly-once charge"
> or "consistency over availability". It says **transaction safety and financial integrity
> *despite the inherently asynchronous nature of external payment networks*** — the same
> idea, but a better sentence, because it names the cause: the processor answers late, or
> not at all. Lead with that, then offer idempotency keys, the state machine and daily
> reconciliation as the mechanisms that deliver it. The local notes' **"99.99999 % uptime"
> should still be dropped** — seven nines is not a claim you can defend. NFRs it puts
> *below* the line: global financial regulation, and extensibility to new payment methods.

## 30. Metrics Monitoring

[notes](30-metrics-monitoring.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/metrics-monitoring)

**Crux** — **One ingest path, two guarantees**: dashboards may be stale, alert evaluation may not.

| Functional | Non-functional |
|---|---|
| 1. The platform can **ingest metrics** — CPU, memory, latency, custom counters — from services<br>2. Users can **query and visualize metrics on dashboards** with filters, aggregations and time ranges<br>3. Users can define **alert rules with thresholds over time windows** ("alert if p99 latency > 500 ms for 5 minutes")<br>4. Users **receive notifications** when alerts fire — email, Slack, PagerDuty | 1. Scale to ingest **5M metrics/s from 500k servers** — ~100–200 bytes per point ≈ **1 GB/s of raw ingestion**<br>2. Dashboard queries return **within seconds**, even for queries spanning days or weeks<br>3. Alerts evaluate with low latency — **`< 1 minute` from metric emission to alert firing**<br>4. Highly available; **eventual consistency is tolerable for dashboards, but alert evaluation must be reliable**<br>5. Handle **late or out-of-order data** gracefully — network delays are common<br>6. ° Cardinality is the real scaling limit — bound it explicitly |

**Below the line** — Log aggregation and full-text search · distributed tracing (spans, traces) · anomaly detection via ML · *NFR below the line:* multi-region replication · strong consistency guarantees

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - 5M metrics/s ≈ 1 GB/s ingest — row-per-point storage is impossible; **cardinality** (unique label sets) is the real limit<br>- Alerts must fire `< 1 min` and be reliable even when the query path is saturated<br>- Dashboard queries over weeks must return in seconds — raw resolution cannot<br>- (staff: late/out-of-order data makes alerts flap or miss windows) | - Push (agents send; simple firewalls) vs pull (Prometheus scrapes; service discovery, natural health check)<br>- Rollups + retention tiers (cheap, lossy) vs raw forever (exact, unaffordable)<br>- Alert evaluation from the TSDB (one path, simple) vs from an in-memory recent window (isolated, reliable)<br>- (staff: build vs Prometheus/Mimir/Thanos on cost per series) | - Agents → collectors → Kafka → TSDB writers (compressed columnar chunks per series, e.g. Gorilla encoding)<br>- Cardinality limits enforced at ingest (reject/drop unbounded labels)<br>- Rollups (1 s → 1 m → 1 h) with retention tiers; dashboards query the right tier<br>- Alert evaluator consumes the stream into an in-memory window per rule; notifier with dedupe and escalation<br>- (staff: alert path isolated from dashboard storms; watermarks bounding late data; cardinality as the failure mode) |

> The split guarantee in NFR 4 is the whole design: a query path that can be stale and an
> alerting path that cannot. Note the sibling problem
> [38. Metrics Aggregation Platform](#38-metrics-aggregation-platform) — same word,
> opposite constraints (pull instead of push, their schema instead of yours).

## 31. Online Chess

[notes](31-online-chess.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/online-chess)

**Crux** — **Stateful, sticky, server-authoritative sessions** — the rare problem in this set where the server holds live state — plus a rank over 10M players.

| Functional | Non-functional |
|---|---|
| 1. Players can find an opponent through **skill-based matchmaking** and start a game<br>2. Players can play a game in real time<br>3. Players can view a **global leaderboard** and see their own rank, both updating shortly after games finish | 1. **Low-latency move propagation — under 200 ms** end to end; in bullet and blitz, players have seconds per move<br>2. **Consistency over availability for game state** — if a game server cannot be reached the game **pauses** rather than letting two clients drift into different board positions: a paused game can be recovered, a corrupted one cannot<br>3. Scale to **500k concurrent games = 1M concurrent connections**<br>4. ° **Latency fairness** — a player on a slow link must not lose on time because of transport; measure the clock server-side from receipt<br>5. ° Game state is durable enough to **reconnect and resume** after a disconnect or server restart<br>6. ° Leaderboard over ~10M players, updated within seconds of a game ending |

**Below the line** — Spectating live games and broadcasting popular boards · in-game chat, friends and social features · puzzles, training, and post-game analysis or replay · tournaments and arena play · anti-cheat and engine detection (fair play) · *NFR below the line:* account security and abuse prevention beyond fair play · GDPR and data privacy · monitoring, logging and alerting · CI/CD and zero-downtime deploys

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - The server holds **live game state** — the rare stateful problem; 1M sockets for 500k games<br>- Move `< 200 ms` with a server-authoritative clock<br>- Rank over 10M players updated per game<br>- (staff: a game server dies mid-game — the game must not diverge) | - Stateless servers + DB per move (simple, slow) vs in-memory owner + journal (fast, needs failover)<br>- Client clock vs server clock on receipt — server, always; fairness of latency is then policy<br>- Wide matchmaking window (fast match, poor quality) vs narrow (slow, good)<br>- (staff: connection tier separated from game logic so sockets do not constrain placement) | - Matchmaking queue by rating bucket with a widening window<br>- Game server owns state in memory, validates moves, measures clock on receipt, journals moves<br>- Rating update on game end; leaderboard in a sorted set<br>- (staff: failover by replaying the move log; pause-don't-diverge on disconnect; latency fairness stated) |

> **Where the derived list differed.** The move budget is **200 ms**, not 100 — the earlier
> row was twice as strict as the question asks. The published list also states the rule the
> derived one only gestured at: **pause, don't diverge.** *"A paused game can be recovered
> and a corrupted one can't"* is the whole justification for choosing CP here, and it is a
> stronger line than "durable enough to reconnect". Latency-fair clocks and the 10M-player
> leaderboard size were inferred — the clock is still the non-obvious requirement, and it
> is what makes this a **stateful, sticky-session** design in a set where nearly everything
> else is stateless. Leaderboard mechanics are shared with
> [34. Game Leaderboard](#34-game-leaderboard). NFRs it puts *below* the line: account
> security and abuse prevention beyond fair play, GDPR, monitoring, CI/CD.

## 32. ChatGPT

[notes](32-chatgpt.md) ⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/chatgpt)

**Crux** — **GPU scheduling.** Strip that away and this is an ordinary CRUD application with streaming.

| Functional | Non-functional |
|---|---|
| 1. Users can send a prompt in a chat and receive an AI-generated response<br>2. Users can view past chats and resume a conversation, with the chat's prior context carried into the prompt | 1. **Low time to first token — `< ~500 ms`**<br>2. **Fault tolerant**: recover an in-flight stream without dropping tokens when a connection drops or a server fails<br>3. **Availability over strong consistency** for conversation state (**~99.9 %+**)<br>4. Scale under **GPU-constrained capacity**, with **fair allocation across a tiered user base** (free, Plus, Pro)<br>5. Scale: **200M DAU, ~20k prompts/s at peak, ~120k streams open at any moment**<br>6. ° Conversation history is durable, and context assembly stays within the model's window and a cost budget |

**Below the line** — Editing or branching existing messages · image, audio or video input and output (text only) · sharing chats or collaborating on a chat · custom GPTs, tool/function calling and web browsing · full-text search across chat history · °training and serving the model itself · *NFR below the line:* durability of every streamed token — only the final assistant message is persisted · authentication, abuse prevention and content moderation · GDPR and data residency · monitoring, logging, alerting and CI/CD

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - GPUs are the scarce resource — 20k prompts/s and 120k live streams must be **scheduled**, not just served<br>- TTFT `< ~500 ms` while the queue is full<br>- Multi-turn context grows past the window<br>- (staff: prefill and decode have different resource profiles — treat them differently) | - Continuous batching (throughput) vs per-request (latency) — continuous batching wins at scale<br>- Reject vs queue vs degrade (smaller model, shorter context) under overload<br>- Truncate vs summarise history — summarise costs tokens, keeps meaning<br>- (staff: fairness across free/Plus/Pro under fixed supply is a business rule the scheduler implements) | - Chat/message store; request → admission-controlled queue → inference server with continuous batching and KV-cache reuse<br>- Tokens streamed via SSE; resumable by message id + offset<br>- Context assembled under a token budget<br>- (staff: prefill/decode disaggregation; prefix caching as the cost lever; tiered degradation) |

> **Where the derived list differed.** The time-to-first-token budget is **~500 ms**, not
> 1 s. And HelloInterview's GPU requirement is sharper than "fair-share scheduling": it
> names **tiering — free, Plus, Pro — as part of the requirement**, which makes admission
> control a product decision with a stated policy rather than a generic queueing problem.
> The reframing still holds: **the expensive resource is GPU-seconds, not storage or
> bandwidth**, so the design question is scheduling and batching, not sharding. Note it
> explicitly puts **durability of every streamed token below the line** — you persist the
> final assistant message, not each chunk — which is a cheaper guarantee than the derived
> row implied. See [37. Local Agent Runtime](#37-local-agent-runtime) for the client side.
> Other NFRs below the line: authn, abuse prevention and moderation; GDPR and data
> residency; monitoring and CI/CD.

## 33. Notification System

[notes](33-notification-system.md) · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/notification-system)

**Crux** — **Fairness and at-least-once delivery across third parties you do not control.**

| Functional | Non-functional |
|---|---|
| 1. Upstream services can send a notification to a user via **push, email or SMS**, either immediately or in the future<br>2. Upstream services can send **campaigns** — the same message delivered to a whole segment of users, immediately or scheduled<br>3. Users can set notification preferences — channel opt-outs and quiet hours | 1. **At-least-once delivery with best-effort deduplication** — better to send a notification twice accidentally than never at all<br>2. Handle **surges of ~5,000 notifications per second** — 10M/day is ~100/s sustained, so the surge is roughly **50×**<br>3. **High-priority notifications (OTPs, security alerts) delivered within 5 s of acceptance, even during a surge**<br>4. ° Per-provider **failure isolation** with retry and backoff — one vendor outage degrades one channel<br>5. ° Scheduled sends fire within a minute of their time, in the user's timezone and outside quiet hours<br>6. ° Preferences are enforced at send time and are authoritative — a suppressed send is a success, not a failure |

**Below the line** — Template management and rich content authoring · delivery analytics dashboards (open rates, click-through tracking) · in-app notification feeds and badge counts · frequency capping across notification types · *NFR below the line:* strict ordering of notifications across channels · compliance workflows around consent and spam regulation · monitoring, alerting and CI/CD

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - A 5k/s campaign blast must not delay an OTP that has a **5 s** budget<br>- Third-party providers (APNs, SMS, email) rate-limit and fail — you do not control them<br>- At-least-once delivery without spamming the user twice<br>- (staff: cost per channel is visible to the business — SMS is money) | - Priority partitions in one queue vs separate pipelines per priority — separate is the only real isolation<br>- Dedupe key carried end to end vs best-effort — carried is the only way to make retries safe<br>- Provider fallback (SMS → push) as policy vs hard-coded<br>- (staff: quiet hours / timezones — respect vs deliverability) | - `POST /notify` with idempotency key → transactional pipeline (OTP) or campaign pipeline, separate queues and quotas<br>- Channel workers with per-provider rate limits, circuit breakers, retries with backoff<br>- Preferences checked at send time; scheduled sends from a timer store<br>- (staff: campaign fan-out on its own pipeline; per-channel fallback policy; cost tracking per channel) |

> **Where the derived list differed.** The priority requirement carries **a number — 5
> seconds — and it must hold *during* a surge**; the derived row stated fairness only
> qualitatively. That number is what forces separate queues or priority partitions, so
> lead with it. HelloInterview also points out the requirements **interact**: best-effort
> deduplication is what makes the surge hard, and saying so out loud is cheap credit. The
> two traffic classes are the heart of the problem — OTPs are low-volume and
> latency-critical, promos are bursty and nobody minds ten minutes — which is why the API
> asks every caller to declare a priority. Per-provider failure isolation, timezone-aware
> quiet hours and "a suppressed send is a success" were local additions, not stated
> requirements. NFRs it puts *below* the line: strict ordering across channels, consent
> and spam compliance, monitoring and CI/CD.

## 34. Game Leaderboard

[notes](34-game-leaderboard.md) ⌀ · no breakdown · ★

**Crux** — **Rank, not top-N.** The top ten is cacheable; *my position among 100M* is the query that hurts.

| Functional | Non-functional |
|---|---|
| 1. Players submit a score at the end of a match<br>2. Anyone can read the **top N** globally and per window — daily, weekly, all-time<br>3. A player can see **their own rank** and the handful of players around them | 1. Top-N read `< 50 ms` at 1M concurrent viewers — it is cached and read far more than written<br>2. **10k score writes/s**, 100M players<br>3. A score is reflected in the rank within **seconds**<br>4. Ties break deterministically (earlier timestamp wins) so ranks are stable between reads<br>5. Window rollover (midnight daily, Monday weekly) happens **without a read outage** — the new window starts empty while the old one is still served |

**Below the line** — Anti-cheat and score validation · matchmaking · rewards and prizes · friend/social leaderboards · historical rank charts

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Top-N is cacheable; **my rank among 100M** is the expensive query<br>- 10k score writes/s into one sorted structure — a hot key<br>- Window rollover (daily/weekly) without a read outage<br>- (staff: exact rank everywhere is unaffordable — decide where approximate is served) | - Redis sorted set (`ZREVRANK`, O(log N), one hot key) vs bucketed counts (O(buckets), approximate within bucket)<br>- Sorted set as source of truth vs derived index over a durable score log — derived means it can be rebuilt<br>- Per-window sorted sets vs one set with timestamps — per window makes rollover a key swap<br>- (staff: exact rank for the top, approximate for the tail) | - Score write → durable log/DB → `ZADD` into sorted set per window<br>- Top-N cached; own rank via `ZREVRANK`<br>- Rollover by creating the next window's set ahead of time<br>- (staff: bucketed histogram for rank on the long tail; rebuild from the score log) |

> ★ No HelloInterview breakdown; requirements written for the set. The trap is assuming
> top-N is the hard query. It is not — it is 10 values you can cache. **"My rank" among
> 100M players** is the hard one, and the honest answers are a sorted set with
> `ZREVRANK`, or bucketed approximate ranks. Shares its spine with
> [22. Top K](#22-youtube-top-k) and [31. Online Chess](#31-online-chess).

## 35. RAG Application

[notes](35-rag-application.md) · no breakdown · ★

**Crux** — **Retrieval quality and ingestion durability.** The LLM call is the least interesting part.

| Functional | Non-functional |
|---|---|
| 1. An API to **upload and update documents**<br>2. Ingestion parses, chunks, embeds and indexes each document, with per-document status visible<br>3. A user query returns a grounded answer **with citations** to the retrieved chunks<br>4. Feedback on answers is captured and improves retrieval | 1. First token `< 1 s`, full answer in a few seconds; the retrieval budget inside that is ~100 ms<br>2. Highly available, eventually consistent — a newly uploaded document becomes searchable within **minutes**<br>3. Ingestion is **resumable per stage**: a failure in embedding does not re-parse a 400-page PDF (this is what [36. Durable Execution](#36-durable-execution-engine) is for)<br>4. Every claim in the answer is **attributable** to a retrieved chunk — no citation, no sentence<br>5. Cost per tenant is bounded: embedding and generation spend are metered and capped<br>6. Deleting a document removes it from retrieval promptly — the compliance requirement |

**Below the line** — Training or fine-tuning the model · serving the model · document-level access-control design beyond the retrieval filter · multimodal documents · agentic multi-hop retrieval

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Retrieval quality decides answer quality — the LLM call is the easy part<br>- Ingestion of millions of chunks must be durable and resumable per document<br>- Answers must be attributable to retrieved chunks<br>- (staff: without evaluation you cannot know whether a change helped) | - Vector-only vs hybrid (BM25 + vector) + reranker — hybrid is almost always better at a latency cost<br>- Fixed-size vs document-aware chunking — aware costs effort, retrieves better<br>- Managed vector DB vs pgvector/ES — one fewer system vs specialised performance<br>- (staff: when not to use RAG at all — fine-tune, long context, or plain search) | - Ingest: parse → chunk (by document type) → embed → store vectors + metadata; per-document per-stage status<br>- Query: embed → hybrid retrieve top-k → rerank → prompt with citations → answer with chunk references<br>- Access control and deletion propagated into the index<br>- (staff: golden set, recall@k, groundedness eval; ingestion as a durable workflow with cost per document) |

> ★ Requirements from the local notes, tightened. The one candidates omit is **NFR 4**:
> without attribution the system is a chatbot, not a RAG system, and the evaluation story
> has nothing to hang on.

## 36. Durable Execution Engine

[notes](36-durable-execution-engine.md) · no breakdown · ★

**Crux** — **State is rebuilt by deterministic replay of an event history** — the whole system is that one idea, defended.

| Functional | Non-functional |
|---|---|
| 1. Register **workflow** and **activity** types; start an execution by type and input<br>2. Schedule activities with per-activity **timeouts** and **retry policies**<br>3. **Durable timers** — sleep for seconds or for six months<br>4. **Signals** (deliver an external event into a running execution) and **queries** (read its state without mutating it)<br>5. **Cancellation** (cooperative, runs compensation) and **termination** (hard kill)<br>6. **Child workflows** and continue-as-new<br>7. **Versioning** — change workflow code while executions are mid-flight<br>8. **Visibility** — list and search executions; export full history for audit | 1. **Durability**: once start returns, the execution is never lost<br>2. **Exactly-once state transitions** in the engine; **at-least-once** activity execution<br>3. Millions of **concurrently open** executions, the vast majority idle<br>4. Task dispatch p99 `< 100 ms` when a poller is waiting<br>5. **Multi-tenant isolation** — namespace quotas, no cross-tenant blast radius<br>6. A history written today is **replayable years later**<br>7. The engine never links or executes user code |

**Below the line** — The workers themselves (customer-owned) · language SDK internals · the business logic in workflows · the scheduling UI

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Millions of open, mostly idle executions — state cannot live in worker memory<br>- Exactly-once state transitions with at-least-once activities<br>- Workflow code changes **while executions are in flight**<br>- (staff: history grows without bound for long-running workflows) | - Event-sourced history + replay (deterministic, replayable for years) vs mutable state row (simple, unauditable)<br>- Workers poll (backpressure, isolation) vs engine calls workers (simpler, coupling)<br>- Exactly-once transitions vs at-least-once activities — the first is the engine's job, the second is idempotency's<br>- (staff: never link user code into the engine — blast radius and multi-tenancy) | - Event history per run (append-only) + mutable cursor; workers poll task queues, replay history to rebuild state, produce next commands<br>- Timers as scheduled tasks; activities retried with idempotency keys<br>- Shard by run id; single-writer ownership per shard<br>- (staff: versioning + determinism rules for code changes; continue-as-new to bound history) |

> ★ See [`36-durable-execution-engine.md`](36-durable-execution-engine.md#requirements).
> NFR 2 and NFR 7 together are the design: workers poll, the engine only moves state, and
> "exactly once" is claimed for the *log*, never for the side effect.

## 37. Local Agent Runtime

[notes](37-local-agent-runtime.md) · no breakdown · ★

**Crux** — **The trust boundary.** The model proposes, the harness disposes, and everything between is policy.

| Functional | Non-functional |
|---|---|
| 1. A multi-turn terminal session: user text in, model text **and actions** out<br>2. Execute actions **locally** — read/write files, run commands, search the repo<br>3. An **extensible** tool surface (MCP): third parties add tools without releasing the host<br>4. **Permission policy** per tool call — ask, allow, deny, allowlist by pattern<br>5. Session **persistence and resume**; a crash must not lose the transcript<br>6. **Subagents** with their own context windows, and **hooks** — deterministic user code on lifecycle points<br>7. Work usefully in a repo far larger than the context window | 1. **The model never executes anything** — it only emits typed requests<br>2. No file leaves the machine except what the harness deliberately puts in the prompt<br>3. A hostile or buggy tool server cannot take over the session<br>4. Tool round trips are the latency floor; the model call is the ceiling<br>5. Cost is dominated by **input** tokens — the same context is resent every turn<br>6. Session state is a local append-only transcript, replayable<br>7. Adding a tool must not require touching the loop |

**Below the line** — Model inference itself · training · the hosted products · multi-user collaboration

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - The model must **never execute anything** — every action crosses a trust boundary the harness owns<br>- Context must fit a token budget across long, resumable sessions<br>- Tool servers may be hostile; prompt injection is privilege escalation<br>- (staff: cost is resent input tokens — every turn re-pays the transcript) | - Permission per call (safe, chatty) vs per session (smooth, wider blast radius)<br>- Model-driven context selection vs harness-driven — the harness owns it<br>- Deterministic hooks vs model discretion for policy — hooks where mistakes are unacceptable<br>- (staff: nested agents with own windows — isolation vs shared context) | - Loop: prompt → model → typed tool call → permission layer → execute → result → append to transcript<br>- Tool registry with typed protocol; transcripts persisted and resumable<br>- Context assembler under a token budget (compaction, caching)<br>- (staff: threat model for tool servers; prompt caching and compaction as the cost lever; deterministic hooks) |

> ★ See [`37-local-agent-runtime.md`](37-local-agent-runtime.md#requirements). NFR 1 is
> the invariant the whole design falls out of: *the model emits intentions, the harness
> performs actions, everything between is policy.*

## 38. Metrics Aggregation Platform

[notes](38-metrics-aggregation-platform.md) · no breakdown · ★

**Crux** — **Correctness on data you pull from APIs you do not own**, under a rate-limit budget that is the real capacity constraint.

| Functional | Non-functional |
|---|---|
| 1. A tenant **connects an account** per provider (OAuth or API key); the platform stores and refreshes credentials<br>2. **Periodic collection** per provider at a configurable cadence, with a historical **backfill** on first connect<br>3. **Normalize** provider responses into one canonical metric schema<br>4. **Aggregate**: time rollups, dimensional group-bys, and cross-provider derived metrics<br>5. **Serve** dashboards and a query API<br>6. Surface **connection health** — expired token, rate-limited, stale stream<br>7. Handle **restatement** when a provider changes a value after collection | 1. **Tiered freshness SLO** — fast connectors p95 `< 5 min`, report-style connectors p95 `< 1 h`; one global number would be a lie<br>2. Query p99 `< 300 ms` per dashboard panel, ~20 panels per dashboard<br>3. **Correctness under at-least-once collection** — refetching a window yields identical aggregates<br>4. **Fault isolation per connection**: one revoked token degrades nothing else<br>5. **10k tenants** with hard quotas on collection, cardinality and query concurrency<br>6. **Repairable** — a mapping bug is fixable for last year's data without re-hitting the provider<br>7. Provider API budget is a **first-class scarce resource**: degrade cadence, never correctness |

**Below the line** — Alerting and anomaly detection · the dashboard front-end · per-provider OAuth app registration · billing · provider-side data quality

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Provider API rate limits are the **capacity** — 10k tenants × N providers share a budget you do not set<br>- Their schema, not yours — mapping bugs are inevitable and must be repairable<br>- Backfill competes with fresh pulls for the same budget<br>- (staff: one freshness number is dishonest across providers with different quotas) | - Pull on schedule vs webhooks — webhooks are fresher and rarely complete; pull is the baseline<br>- Store normalised only vs retain raw responses — raw costs storage, buys repairability without re-fetching<br>- Overwrite facts vs version them — versioning makes restatements auditable<br>- (staff: build vs Fivetran/Airbyte — and where they break) | - Connectors per provider; per-connection scheduler with quota; fetch → raw store (S3) → mapping → canonical schema<br>- Idempotent upserts keyed by `(tenant, provider, window)`; backfill on its own low-priority lane<br>- Connection health and freshness surfaced per tenant<br>- (staff: raw retention for replay; versioned facts; per-connection fault isolation; tiered freshness SLOs) |

> ★ See [`38-metrics-aggregation-platform.md`](38-metrics-aggregation-platform.md#requirements).
> The contrast with [30. Metrics Monitoring](#30-metrics-monitoring) is the answer to
> "why is this not just a monitoring system": **pull not push, their schema not yours,
> rate limits not traffic, restatable not immutable.**

## 39. Search Autocomplete

[notes](39-search-autocomplete.md) · no breakdown · ★

**Crux** — It is a **ranking and freshness problem at 220k QPS inside a 50 ms budget**, not a trie exercise.

| Functional | Non-functional |
|---|---|
| 1. Given a prefix, return the **top K = 10** suggestions by relevance<br>2. Candidates come from **multiple verticals** — query logs, a product catalogue, the user's own history<br>3. Match **mid-query**, not only from the first character<br>4. **Typo tolerance**<br>5. **Trending** — a query spiking now appears within minutes<br>6. **Personalization and context** — history, locale, session, category scope<br>7. **Catalogue fidelity** — a delisted product stops being suggested quickly<br>8. **Suppression** — unsafe, defamatory or PII-bearing strings never appear; takedowns apply within seconds<br>9. The system **improves from its own usage**, auditably | 1. **p99 `< 50 ms` server, `< 100 ms` end to end** — a perceptual budget that includes the network<br>2. **~220k peak QPS** — 6× your search traffic, derived not assumed<br>3. **99.99 % with graceful degradation** — a shorter or staler list is fine, a slow one is worse than none<br>4. **Asymmetric freshness**: trending and new products `≤ 5 min`, **removals `≤ 30 s`**<br>5. **Read-only serving path** — no synchronous writes, no database on the request path<br>6. Relevance is measurable — offline replay plus online interleaving<br>7. Multi-language, multi-script, multi-marketplace |

**Below the line** — The search results page and its retrieval/ranking · post-submission spell correction · indexing the corpus being searched

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - p99 `< 50 ms` at 220k QPS — every keystroke fires a request; nothing synchronous can sit on the path<br>- Ranking (popularity, personalisation, trend) is the product; the trie is the easy part<br>- Removals (offensive/legal) must land in seconds while everything else may be stale<br>- (staff: the popularity feedback loop reinforces itself — top suggestions stay top) | - Trie with top-K per node (fast, rebuild cost) vs search-engine prefix query (flexible, slower)<br>- Precomputed top-K (stale, fast) vs live ranking (fresh, over budget)<br>- Client debounce (fewer requests, slight lag) vs per-keystroke<br>- (staff: shorter list vs slower list under load — shorter) | - Offline: query logs → aggregation → candidate + score → trie/prefix index with top-K per prefix, rebuilt periodically, pushed to serving nodes<br>- Online: prefix lookup + fuzzy candidates → rank → cache; client debounce<br>- Denylist fast path at the edge for removals<br>- (staff: exploration and position-bias correction; offline replay/interleaving evaluation) |

> ★ See [`39-search-autocomplete.md`](39-search-autocomplete.md#nfr). NFR 4 is the
> unusual one and worth stealing for other problems: **AP for everything except removals.**
> A single asymmetry in the consistency requirement, which then explains the denylist fast
> path and the two-tier index.

## 40. Full-Text Document Search

[notes](40-document-search.md) · no breakdown · ★

**Crux** — **Choosing the indexing unit**, and holding two different SLOs for two different query shapes.

| Functional | Non-functional |
|---|---|
| 1. Index documents made of text lines, with metadata (path, author, repo, mtime, tags)<br>2. `search(query) → documents` containing the query<br>3. Query forms: term, **phrase**, boolean, prefix/wildcard, field-scoped<br>4. **Per-document evidence** — which lines matched, highlighted<br>5. Metadata filters combinable with the text query<br>6. Return **all** matches (paginated or streamed) **or** the top K — caller's choice<br>7. Documents are created, updated and deleted; the index tracks them | 1. **p99 `< 200 ms`** for a top-K query over **100M documents**<br>2. Exhaustive retrieval of a 1M-document result set completes without destabilising the cluster — an explicitly weaker SLO<br>3. **Near-real-time**: searchable within ~1 s of indexing<br>4. **5k docs/s** sustained indexing, 10× during a bulk reindex<br>5. The index is **derived and rebuildable** — losing it is an availability incident, never data loss<br>6. Result sets are **stable across pagination** — no duplicates, no skips |

**Below the line** — Semantic/vector retrieval (see [35. RAG](#35-rag-application)) · personalization · the crawler or ETL producing the documents · access-control design beyond the filter that enforces it

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Choosing the **indexing unit** — file vs line vs section — decides phrase queries and result shape<br>- Two query shapes: top-K in `< 200 ms` and exhaustive scans with a weaker SLO<br>- 5k docs/s indexed with bounded visibility lag<br>- (staff: a search engine is the wrong tool for some of these queries — name them) | - Document-per-file with position gaps vs document-per-line — per-file keeps phrases from spanning lines wrongly<br>- Refresh interval (visibility) vs indexing throughput<br>- Index as source of truth vs derived and rebuildable — derived settles the consistency argument<br>- (staff: shard size vs scatter-gather tail latency) | - Ingest → replayable log → indexer → sharded inverted index (ES/OpenSearch); refresh interval tuned<br>- Query: parse → filters in filter context (cached) → top-K with highlighting → cursor pagination<br>- Exhaustive queries via point-in-time cursor under a separate SLO<br>- (staff: rebuild from the log; shard sizing; queries a search engine should not serve) |

> ★ See [`40-document-search.md`](40-document-search.md#requirements). NFR 5 is the
> sentence that wins the consistency argument: **Elasticsearch is not your database.**
> Two SLOs for two query shapes (NFR 1 vs 2) is also worth copying — it is how you avoid
> promising `< 200 ms` on a query that legitimately cannot meet it.

## Flash Sale (no local notes)

⌀ · [breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/flash-sale)

**Crux** — **Admission control.** Ten million requests must not reach the inventory row at all.

| Functional | Non-functional |
|---|---|
| 1. Users can view the flash sale item<br>2. Users can **secure/reserve an available unit for a limited time** while the sale is active<br>3. Users who secure a unit can complete payment to purchase it | 1. **Prevent overselling**, even when many people are trying to claim the same unit at the same moment<br>2. Handle **millions of users arriving within seconds** of the sale starting<br>3. Give users a **fair opportunity to compete** for the limited inventory<br>4. ° Reservations **expire** and return stock automatically<br>5. ° The read path survives the write path — viewing the item stays up even when reservations are being rejected en masse<br>6. ° HI publishes **no figures**; the notes assume 10M users against 10k units |

**Below the line** — Browsing or searching a broader product catalogue · shipping, returns, refunds, order history · administrators creating, configuring or managing flash sales · *NFR below the line:* protecting user data and adhering to regulations like GDPR · fault tolerance · secure transactions for purchases

| Key bottlenecks (crux) | Key tradeoffs | Key design expected |
|---|---|---|
| - Millions of requests in seconds must **not reach the inventory row at all**<br>- Never oversell; the decrement must be atomic<br>- Fairness must be demonstrable, not just claimed<br>- (staff: bots — without mitigation the fair queue is a bot queue) | - Counter in memory as the gate (fast, needs reconciliation) vs DB row (correct, melts)<br>- Waiting room with signed tokens (bounded arrivals, adds a step) vs open door<br>- Reservation with TTL (returns stock on abandonment) vs immediate decrement<br>- (staff: sharded counter avoids one hot key but needs tail reconciliation when shards are uneven) | - Item page fully static on CDN; waiting room issues signed tokens at a bounded rate<br>- Token holders hit an in-memory atomic counter (Redis `DECR`) → reservation with TTL → order persisted async<br>- Sold-out response served from the counter without touching the DB<br>- (staff: sharded counter + reconciliation; bot mitigation as an explicit requirement; which side may lie when gate and ledger disagree) |

> **Where the derived list differed.** The published NFRs are only three, and all three are
> stated qualitatively — **no numbers at all**, so "10M attempts on 10k units" was the
> earlier row's invention. Ask the interviewer for the figures. Reservation expiry and
> read-path survival are not stated either; they are good instincts, but they belong in the
> deep dive rather than on the board as quoted requirements. What *is* published and worth
> keeping is **fairness as a first-class requirement** — it sits beside overselling, which
> is what makes the queue a design element rather than a fallback.
>
> The one callout worth reading in full is on the functional side: **the reservation step is
> the requirement candidates most often miss.** Payment runs through a third party and takes
> seconds, and you cannot hold inventory in an open database transaction for that long — the
> reservation is what takes a unit off the table immediately so the money can settle
> afterward. Arriving there yourself scores better than being led to it.
>
> No local file exists for this one yet; it is [5. Ticketmaster](#5-ticketmaster) with the
> contention compressed into a single second and the seat map removed. NFRs it puts *below*
> the line: GDPR and user-data protection, fault tolerance, secure transactions.

---

## Patterns across the set

### The five NFR archetypes

Nearly every problem here is one of five shapes. Recognising which one you are in during
the requirements phase tells you what the deep dives will be.

| Archetype | Defining NFR | Problems |
|---|---|---|
| **AP read-heavy fan-out** | Stale is fine; the feed must be precomputed | [6](#6-instagram), [7](#7-fb-news-feed), [20](#20-news-aggregator), [21](#21-price-tracking-service), [3](#3-yelp) |
| **CP contention on a single row** | Two people, one unit; a hold with a TTL | [4](#4-local-delivery-service-gopuff), [5](#5-ticketmaster), [14](#14-online-auction), [23](#23-uber), [29](#29-payment-system), [Flash Sale](#flash-sale-no-local-notes) |
| **Lossless high-throughput ingest** | Nothing may be dropped, nothing double-counted | [22](#22-youtube-top-k), [26](#26-web-crawler), [27](#27-ad-click-aggregator), [30](#30-metrics-monitoring), [38](#38-metrics-aggregation-platform) |
| **Real-time bidirectional delivery** | Persistent connections, `< 500 ms`, presence | [10](#10-whatsapp), [19](#19-fb-live-comments), [25](#25-google-docs), [31](#31-online-chess), [32](#32-chatgpt) |
| **Big-object movement** | The object is bigger than the request | [2](#2-dropbox), [17](#17-youtube) |

### Below-the-line items that recur everywhere

Say these are out of scope rather than listing them as non-functional requirements. They
appear as excluded in most of the published breakdowns, for the same reason: they are real,
they are not the design, and claiming them as NFRs signals you have not chosen.

| Recurring exclusion | Appears in |
|---|---|
| Authentication, accounts, profiles | 1, 9, 11, 21 and implicitly everywhere |
| Payments and billing | 4, 9, 14, 23, Flash Sale |
| Moderation, fraud, spam, anti-cheat | 3, 17, 19, 27, 31, 39 |
| GDPR, data protection, security review | 3, 4, 5, 23 |
| CI/CD, backups, monitoring and alerting | 5, 8, 9, 17, 23 |
| Analytics and reporting dashboards | 1, 9, 13, 29, 33, 38 |
| Search, when the problem is not about search | 4, 14, 17, 21, 32 |

### Numbers worth having in your head

The latency figure the published breakdowns use most often is **`< 500 ms`** for a user-facing
read, **`< 100 ms`** for a redirect or a real-time broadcast, and **`< 10 ms`** for anything
sitting in the path of every request ([12](#12-distributed-cache), [13](#13-rate-limiter)).
Freshness is usually **1 minute** ([7](#7-fb-news-feed), [22](#22-youtube-top-k),
[28](#28-fb-post-search), [30](#30-metrics-monitoring)) — long enough for a batch, short
enough to feel live. When you are asked for a number and have no basis, those three are
defensible starting points; say which one you picked and why.

## The superset: every crux collapsed into one list

Thirty-nine cruxes, but they are not thirty-nine skills. They are the same fourteen moves
in different costumes. This is the transferable part of the page: if you can perform these
on demand, you can take a problem that is not on the list.

| # | The move | What it sounds like | Where it *is* the crux |
|---:|---|---|---|
| 1 | **Name the constraint that decides the architecture** — in minute five, before any boxes | "Exact counting at 700k events/s is the tension; everything I draw serves it" | [22](#22-youtube-top-k), [26](#26-web-crawler), [32](#32-chatgpt), [2](#2-dropbox), [13](#13-rate-limiter), [12](#12-distributed-cache) |
| 2 | **Derive the numbers; never accept one** | "Every search fires six autocomplete requests, so this is 6× search traffic, not a fraction" | [39](#39-search-autocomplete), [27](#27-ad-click-aggregator), [28](#28-fb-post-search), [18](#18-job-scheduler), [6](#6-instagram) |
| 3 | **Split the system by guarantee, not by noun** | "Browsing is AP, booking is CP — two paths, two designs, one product" | [5](#5-ticketmaster), [4](#4-local-delivery-service-gopuff), [24](#24-robinhood), [30](#30-metrics-monitoring), [40](#40-full-text-document-search), [39](#39-search-autocomplete) |
| 4 | **Make writes idempotent, and say where the dedupe key comes from** | "The client supplies the key; the retry is free because the second write is a no-op" | [29](#29-payment-system), [27](#27-ad-click-aggregator), [18](#18-job-scheduler), [33](#33-notification-system), [36](#36-durable-execution-engine), [11](#11-strava) |
| 5 | **Prefer a single writer to a distributed lock** | "All bids for one auction go to one partition, so ordering is free" | [14](#14-online-auction), [23](#23-uber), [25](#25-google-docs), [36](#36-durable-execution-engine), [31](#31-online-chess) |
| 6 | **Treat derived state as derived** — rebuildable, replayable, and allowed to be lost | "The index is not the database; losing it is an availability incident, not data loss" | [40](#40-full-text-document-search), [23](#23-uber), [34](#34-game-leaderboard), [6](#6-instagram), [12](#12-distributed-cache) |
| 7 | **Bound the hot key and the skew explicitly** | "One video takes 5% of the traffic, so I salt the partition and merge on read" | [22](#22-youtube-top-k), [12](#12-distributed-cache), [14](#14-online-auction), [20](#20-news-aggregator), [24](#24-robinhood), [34](#34-game-leaderboard) |
| 8 | **Design the degradation, not just the happy path** | "It fails open, and here is the abuse window that buys" | [13](#13-rate-limiter), [39](#39-search-autocomplete), [32](#32-chatgpt), [24](#24-robinhood), [4](#4-local-delivery-service-gopuff) |
| 9 | **Precompute the read path; keep the write path honest** | "The feed is a materialized view with a bounded window and a backfill on deep scroll" | [6](#6-instagram), [7](#7-fb-news-feed), [3](#3-yelp), [20](#20-news-aggregator), [34](#34-game-leaderboard), [39](#39-search-autocomplete) |
| 10 | **Hand the product decision back to the product** — and say that you are doing it | "Whether trading continues on a stale price is a compliance decision; the design supports either" | [24](#24-robinhood), [10](#10-whatsapp), [14](#14-online-auction), [18](#18-job-scheduler), [19](#19-fb-live-comments), [8](#8-tinder), [33](#33-notification-system) |
| 11 | **Cost is a design input, not a postscript** | "99% of watch time is on 1% of videos, so the long tail lives in cold storage and transcodes lazily" | [17](#17-youtube), [32](#32-chatgpt), [30](#30-metrics-monitoring), [2](#2-dropbox), [38](#38-metrics-aggregation-platform), [33](#33-notification-system) |
| 12 | **Design for repair and evolution** — bugs found later must be fixable | "Raw responses are retained, so a mapping bug is fixable for last year without re-fetching" | [38](#38-metrics-aggregation-platform), [36](#36-durable-execution-engine), [29](#29-payment-system), [40](#40-full-text-document-search), [6](#6-instagram) |
| 13 | **Build versus buy, with the line where the bought thing breaks** | "Run Redis Cluster; here is what fails first at 1 TB, and that is when this conversation restarts" | [12](#12-distributed-cache), [30](#30-metrics-monitoring), [38](#38-metrics-aggregation-platform), [40](#40-full-text-document-search), [35](#35-rag-application) |
| 14 | **Scope out loud** — what you are not building, and why | "Auth, payments and moderation are out; each is a system, none is this one" | every problem on this page |

### How the superset maps to the levels

- **Mid** is expected to reach the *complete design* — move 9 and a clean data flow, with move 2's numbers written down even if not derived. A mid answer that does move 1 well is a strong one.
- **Senior** is moves **1–9**. That is the honest summary of the bar: find the deciding constraint, quantify it, split the system by guarantee, and be specific about idempotency, ownership, skew and degradation. Every Senior point above is an instance of one of those nine.
- **Staff+** adds moves **10–14**, and they share a theme: *consequences outside the diagram.* Who owns the decision, what it costs to run, how it is repaired when you are wrong, what you would buy instead, and what you are explicitly not doing. A staff answer is rarely a cleverer diagram — it is the same diagram with the sacrifices named.

### In the room

The first ten minutes, in order, using this page as the source:

1. Three or four **functional requirements** as user sentences — [the rules](#what-makes-a-requirement-ideal).
2. **Out of scope**, said aloud, before anyone asks.
3. **Numbers**, derived — DAU, request rates, object sizes, read:write ratio.
4. Three or four **NFRs**, each with a number or an explicit choice, and one CAP sentence that names the *operation*.
5. **The crux** — one sentence: "the thing that decides this design is X." Then build toward it, and let the interviewer know that is what you are doing.

## Related

- [HLD interview framework](00-interview-framework.md) — the flow, and the SCALE checklist these NFRs are filtered through
- [HLD index](README.md) — the problems themselves
- [Database decision list](../appendix/database-decision-list.md#problem-set-database-map) — which store each of these requirement sets lands on
- [Scaling strategies](../appendix/scaling-strategies.md) — the mechanisms the NFRs above buy
- [Isolation levels](../appendix/isolation-levels.md) — for every row that says "strongly consistent"
