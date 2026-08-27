# Isolation Levels

*From the [HLD quick cheat sheet](https://docs.google.com/spreadsheets/d/1OOnBR6kKoLRFBjePAI9D-Q152yAWHgqYOaWh_ma_KfY/edit?gid=734431012#gid=734431012)
spreadsheet, `Isolation levels` tab — transcribed here so it reads offline.*

Four levels, four anomalies, and the rule for picking one. The short version: pick the
weakest level that still protects the invariant you actually care about.

## A) The four levels

| Level | Solves | Still allows (problem) | Locking | In one line |
|---|---|---|---|---|
| **Read Uncommitted** | Maximum concurrency; almost no waiting. | Dirty reads, non-repeatable reads, phantom reads, write skew. | — | I may see even uncommitted changes. |
| **Read Committed** | Prevents dirty reads. Every query sees only committed data. | Non-repeatable reads, phantom reads, write skew. | Read locks released after each statement. | A transaction never sees someone else's unfinished work. |
| **Repeatable Read** | Prevents dirty reads and non-repeatable reads. Previously read rows remain unchanged for the transaction. | Phantom reads (in traditional locking implementations), write skew. PostgreSQL's MVCC implementation also prevents many phantom scenarios. | Read locks held until commit. | Rows I've already read won't change during my transaction. |
| **Serializable** | Prevents all anomalies by making concurrent transactions behave like a serial execution. | Lower concurrency, blocking, deadlocks, transaction retries, reduced throughput. | Read locks + range/predicate protection (or equivalent conflict detection). | The transaction feels like I have the database to myself. |

**Example of what each level lets through**

| Level | Example |
|---|---|
| Read Uncommitted | Read a balance that is later rolled back. |
| Read Committed | Read the balance twice and get ₹100 then ₹150, because another transaction committed in between. |
| Repeatable Read | Query "employees with salary > ₹1L" returns 10 rows, later returns 11 because a new employee was inserted. |
| Serializable | Two users trying to book the last seat — one transaction is delayed or aborted. |

## B) Anomaly matrix

✅ = problem can occur  ❌ = prevented

| Isolation Level | Dirty Read | Non-repeatable Read | Phantom Read | Write Skew |
|---|:---:|:---:|:---:|:---:|
| Read Uncommitted | ✅ | ✅ | ✅ | ✅ |
| Read Committed | ❌ | ✅ | ✅ | ✅ |
| Repeatable Read | ❌ | ❌ | ✅ \* | ✅ |
| Serializable | ❌ | ❌ | ❌ | ❌ |

\* Traditional locking-based Repeatable Read allows phantom reads. Some MVCC
implementations (e.g. PostgreSQL) prevent many phantom scenarios.

## C) The four anomalies

| Anomaly | What it is | Worked example |
|---|---|---|
| **Dirty read** | A transaction reads data written by another transaction before it commits. If the other transaction rolls back, the read value never actually existed. | T1 updates balance to ₹200 (not committed). T2 reads ₹200. T1 rolls back. Actual balance is ₹100. |
| **Non-repeatable read** | A transaction reads the same row twice and gets different values, because another transaction committed an update in between. | T1 reads balance = ₹100. T2 updates balance to ₹150 and commits. T1 reads again → ₹150. |
| **Phantom read** | A transaction executes the same query twice and gets a different *set of rows*, because another transaction inserted or deleted matching rows. | T1: `SELECT * FROM Orders WHERE amount > 1000` → 10 rows. T2 inserts a ₹2000 order. T1 runs the same query again → 11 rows. |
| **Write skew** | Two transactions read the same data, update *different* rows, and together violate a business rule because neither transaction detects a conflict. | Rule: at least one doctor must remain on call. T1 turns Doctor A off. T2 turns Doctor B off. Both commit. Result: no doctor is on call. |

## D) Picking a level by use case

### Read Committed is enough

| Use case | What it does | Why Read Committed is sufficient | Why higher isolation is unnecessary |
|---|---|---|---|
| **User profile page** | Display a user's profile information (name, email, phone, etc.). | The request typically performs a single read. Seeing the latest committed value is enough — if another transaction updates the profile immediately after the read, that's acceptable. | Holding a transaction-wide snapshot or serializing access provides no additional business value and only reduces concurrency. |
| **Product catalog / e-commerce listing** | Display products, prices, ratings and descriptions. | Users only need a valid committed view of the catalog. Prices or inventory may naturally change between requests. | Keeping a consistent snapshot for the entire browsing session wastes resources. Final validation happens during checkout anyway. |
| **News feed / social feed** | Fetch latest posts, comments and likes. | New posts arriving while the user is scrolling are expected behavior. Every query should simply return committed data. | Users don't expect the feed to remain frozen while they're browsing. |
| **Monitoring dashboard** | Display metrics like CPU usage, active users, request count. | The dashboard should avoid showing partially committed values, but slight changes between refreshes are perfectly normal. Read Uncommitted is fine if approximate values are acceptable. | A stable snapshot provides little benefit because metrics change continuously. |
| **Inventory display** | Show "Items remaining: 12". | Displaying a recently committed inventory count is sufficient. Inventory will be validated again before purchase. | Locking inventory just to display it would unnecessarily reduce throughput. |

### Repeatable Read — you need one consistent snapshot

| Use case | What it does | Why Repeatable Read is needed | Why higher isolation is unnecessary |
|---|---|---|---|
| **Bank statement generation** | Generate a monthly statement by reading transactions, computing totals and balances. | Every query must observe the same snapshot so the statement is internally consistent from beginning to end. | Other customers should still be able to transact. We only need a snapshot, not exclusive access to the database. |
| **Financial / sales report** | Calculate revenue, expenses, taxes and profit using multiple queries. | All calculations must use the same version of the data; otherwise totals won't match. | Preventing concurrent updates is unnecessary — the report simply represents the database at a particular point in time. |
| **PDF / invoice generation** | Read customer details, order items and totals across multiple queries. | The generated document should be based on one consistent snapshot. | The application doesn't care if new orders arrive after generation starts. |

### Serializable — a business rule is at stake

| Use case | What it does | Why Serializable is needed | Why weaker isolation fails |
|---|---|---|---|
| **Seat booking (flight, movie, train)** | Ensure only one customer can reserve the last available seat. | Multiple concurrent transactions could otherwise reserve the same seat. The rule requires transactions to behave as if executed one at a time. | No weaker isolation level guarantees that concurrent transactions won't violate this invariant. |
| **Inventory reservation** | Reserve the last remaining unit of a product. | Prevents overselling when multiple customers purchase simultaneously. | Read Committed or Repeatable Read can still allow concurrent transactions to oversell, unless additional locking or constraints are used. |
| **Auction / bid processing** | Accept bids while ensuring the highest valid bid wins correctly. | Concurrent bids must be evaluated atomically to maintain auction correctness. | Weaker isolation may lead to lost updates or inconsistent winners. |
| **Doctor scheduling / on-call roster** | Ensure at least one doctor remains on call at all times. | The business rule spans multiple rows; concurrent updates can violate the invariant (write skew). | Repeatable Read still allows write skew in many implementations. |

### Read Uncommitted (rare)

| Use case | What it does | Why it's acceptable | Why higher isolation is unnecessary |
|---|---|---|---|
| **Analytics / trend dashboard** | Display approximate counts or trends where perfect accuracy isn't critical. | Occasional dirty reads have negligible business impact and maximum throughput is desired. | Waiting for committed data only increases latency without improving decision-making. |

## E) Decision tree

```
Am I okay reading uncommitted data?
│
├── Yes
│    ↓
│   Read Uncommitted
│
└── No
     │
     ▼
Do I only execute one query
(or don't care if later queries see newer committed data)?
│
├── Yes
│    ↓
│   Read Committed
│
└── No
     │
     ▼
Do I just need a consistent snapshot for reporting or calculations?
│
├── Yes
│    ↓
│   Repeatable Read
│
└── No
     │
     ▼
Can concurrent transactions violate a business rule?
│
├── Yes
│    ↓
│   Serializable
│
└── No
     ↓
    Repeatable Read
```

## Related

- [Database decision list](database-decision-list.md) — when a workload needs distributed SQL
- [HLD appendix](hld.md) — consistency models, Elasticsearch, Cassandra, Kafka, Redis, Iceberg, Pinot
- [HLD problems](../hld/README.md)
