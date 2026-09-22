# Data modelling — Star schema, Data Vault 2.0, OBT

[← Data engineering index](README.md) · [All docs](../README.md)

---

Written for the FinBox Lead DE JD line *"Expertise in data modelling: Star Schema, Data
Vault 2.0 and OBT"* ([FinBox HM round](../finbox/01-hm-round.md)). The honest position: you
have modelled financial facts as star-shaped ledgers and served OBT tables from Pinot; Data
Vault is conceptual. This doc is the ramp that makes "conceptual" sound like judgement rather
than a gap — and a worked lending-domain model to reason with in the room.

| | |
|---|---|
| [1) The 60-second answer](#1-the-60-second-answer) | [2) Star schema](#2-star-schema) |
| [3) Data Vault 2.0](#3-data-vault-20) | [4) OBT — one big table](#4-obt--one-big-table) |
| [5) How they compose](#5-how-they-compose) | [6) A lending-domain model, worked](#6-a-lending-domain-model-worked) |
| [7) Questions an HM will ask](#7-questions-an-hm-will-ask) | [8) Map to what you've built](#8-map-to-what-youve-built) |
| [9) Vocabulary to have cold](#9-vocabulary-to-have-cold) | |

## 1) The 60-second answer

If asked "walk me through the three":

> "They solve different problems and usually coexist. **Star** is for *analysis*: facts at a
> declared grain, surrounded by dimensions, optimised for humans and BI to ask questions.
> **Data Vault** is for *integration and audit*: hubs for business keys, links for
> relationships, satellites for attributes with full history — insert-only, source-agnostic,
> so you can absorb many changing sources without rewriting the model, at the cost of being
> painful to query directly. **OBT** is for *serving*: one wide, pre-joined table at one grain
> so a dashboard or a model reads one thing with no joins — cheap in columnar stores,
> brittle if you treat it as the source of truth.
>
> The pattern I'd run: raw → vault or a staging/integration layer → star marts → OBT for
> serving and features. Vault only where source churn and audit justify it — for a lender
> integrating hundreds of partners with a regulator on the other side, that's a real case."

## 2) Star schema

**Shape.** A fact table (measurements, numeric, at a declared grain) joined to dimension
tables (context: who, what, when, where) by surrogate keys. Denormalised dims (one hop);
*snowflake* normalises dims further (dim_product → dim_category) — saves space, costs joins,
almost never worth it in a columnar warehouse.

**Grain is the whole game.** State it unprompted: *"one row per loan application"*, *"one
row per bank-statement transaction"*, *"one row per borrower per day"*. Every bug in a star
schema is a grain bug: a join that multiplies rows, a measure summed across the wrong grain,
a dimension attribute that actually varies at a finer grain than the fact.

**Fact table types** — know all three, most candidates know one:

| Type | Grain | Example (lending) | Additive? |
|---|---|---|---|
| **Transaction** | one row per event | `fact_bank_txn`: one statement line; `fact_decision`: one decision on one application | fully additive |
| **Periodic snapshot** | one row per entity per period | `fact_loan_balance_daily`: outstanding principal per loan per day | semi-additive (sum across loans yes, across days no) |
| **Accumulating snapshot** | one row per process instance, updated as it moves | `fact_application_funnel`: applied → bureau → decision → KYC → disbursed, with a timestamp column per stage | measures are lag/durations |

Also: **factless facts** (an event with no measure — "borrower consented to AA at time T"),
**degenerate dimensions** (an id kept on the fact with no dim table — application number),
**junk dimensions** (a handful of low-cardinality flags collapsed into one dim), **role-playing
dimensions** (one `dim_date` used as application_date, decision_date, disbursal_date via
aliases), **conformed dimensions** (one `dim_borrower` shared by every fact so marts agree),
**bridge tables** (many-to-many: a loan with co-applicants).

**Slowly changing dimensions** — the mechanics, not just the numbers:

- **Type 1** overwrite. Corrections. Loses history.
- **Type 2** new row per change, with `valid_from / valid_to / is_current` (or a surrogate key
  per version). The merge: close the current row (`valid_to = now, is_current = false`),
  insert the new one. Facts carry the surrogate key of the version that was current *at fact
  time* — that's how you get as-of correctness for free. On Iceberg this is exactly `MERGE
  INTO ... WHEN MATCHED AND hash_diff <> new_hash THEN UPDATE ... WHEN NOT MATCHED THEN
  INSERT` plus the insert of the new version.
- **Type 3** previous-value column. Rare; "old vs new region" reports.
- Hybrid (6 = 1+2+3) exists; mention it only if asked.

**Late-arriving data.** Late *facts*: the dim version to attach is the one valid at the
fact's event time, not load time — so keep `valid_from/valid_to` and do an as-of join. Late
*dims* (a fact arrives before its borrower record): insert a placeholder dim row with the
natural key, backfill attributes when they arrive (SCD1 on the placeholder).

**Surrogate vs natural keys.** Surrogate (int or hash) on dims so SCD2 versions are
distinguishable and source keys can change; natural key kept as an attribute. Hash surrogates
(`md5(natural_key)`) let you load facts and dims in parallel without lookups — which is the
bridge to Data Vault.

**Where star hurts.** Many source systems describing the same entity (which one wins in
`dim_borrower`?), source schema churn (every change ripples into the dim), audit questions
("what did we know about this borrower on the 3rd, from which source?"). Those are the Vault
arguments.

## 3) Data Vault 2.0

**Shape.** Three table types, insert-only, every row stamped with `load_date` and
`record_source`:

| Table | Holds | Key |
|---|---|---|
| **Hub** | one row per business key ever seen (borrower, loan, account, lender) — the key and nothing else | `hub_key = hash(business_key)` |
| **Link** | one row per relationship instance between hubs (loan ↔ borrower, account ↔ borrower, application ↔ lender ↔ product) | `link_key = hash(hub keys)` |
| **Satellite** | descriptive attributes for a hub or link, **one satellite per source system (or per rate of change)**, full history via `load_date` + `hash_diff` | hub/link key + `load_date` |

*"2.0"* specifically adds: hash keys (so loads are parallel and source-order-independent),
`hash_diff` for change detection, the raw-vault / business-vault split, and PIT/bridge
tables for query performance.

**Why it exists.** The integration problem. A borrower is described by KYC, by a bank
statement parser, by a device SDK, by a bureau pull, by the lender's LOS — five sources,
five schemas, five refresh cadences, and they disagree. In a star you must decide which wins
before you load. In a vault you don't: one `hub_borrower`, five satellites, every version of
every source kept, and the "which wins" rule lives in the **business vault** (computed
satellites) or the marts, where it can change without reloading anything. A new source is a
new satellite, not a schema migration.

**Why it's painful.** Querying it directly means joining hub → several satellites (each
needing a "latest per key" or as-of subquery) → link → hub → satellites. That's why **PIT
(point-in-time)** tables (per hub, per snapshot date, the satellite `load_date` to use — turns
the as-of subqueries into equi-joins) and **bridge** tables (pre-joined hub–link–hub paths)
exist, and why nobody serves BI from a raw vault. It also demands discipline: hash key
standards, satellite splitting rules, load patterns. Teams that adopt it half-way get the
joins without the benefits.

**When it wins — say these, they are the judgement:**

- Many source systems for the same entities, with disagreements you must preserve.
- **Audit / regulatory**: "what did we know, when, from where" is a first-class query.
- Source schema churn — you don't control upstream (hundreds of partner integrations).
- Parallel, restartable loads (hash keys → no lookups → no ordering).
- Deletion and consent: satellites are per-source, so purpose-scoped data can be isolated
  and dropped or crypto-shredded without touching the rest.

**When it loses:** a small number of sources you control, analytics-first consumers, a team
without the modelling discipline — a star with SCD2 gets you 90% at a third of the joins.

**Raw vault vs business vault.** Raw = source data restructured, no business rules, fully
auditable. Business = derived satellites/links applying rules (which income figure wins,
derived risk bands, deduplicated identities). Rules change → recompute business vault; raw
untouched.

## 4) OBT — one big table

**Shape.** One wide, denormalised table at a single grain with every attribute a consumer
needs already joined in — dimension attributes copied onto the fact rows. Columnar storage
makes the width nearly free (you scan the columns you read); the joins are paid once at
build time instead of on every query.

**Where it belongs.** The *serving* edge: BI dashboards, self-serve analytics, ML feature
tables (one row per borrower per as-of date with every feature as a column), API-backed
reports. Pinot, ClickHouse and BigQuery are at their best on OBT; a star with runtime joins
is at its worst on Pinot (no joins to speak of) and mediocre on ClickHouse.

**The trade-offs to say out loud:**

- **Attributes are frozen at build time.** That's a *feature* for facts ("the borrower's
  income band as of decision time") and a *bug* if consumers expect current attributes.
  Decide per column: as-of or current — and if current, the OBT is rebuilt or the column
  is a lookup.
- **Not a source of truth.** OBT is derived; rebuildable from the star/vault. The moment
  someone writes to it directly you've lost the ability to reproduce it.
- **Recompute cost.** A dim change can touch every row — so OBTs are partitioned (by date,
  by tenant) and rebuilt incrementally by partition, which is exactly the incremental
  materialisation problem you solved on Iceberg.
- **Width and cardinality.** Hundreds of columns are fine; high-cardinality string columns
  repeated per row are not (dictionary encoding helps; a numeric key plus a lookup often
  helps more).
- **Multiple grains → multiple OBTs.** One OBT per consumer grain (per application, per
  transaction, per borrower-day). Don't make one table serve two grains.

## 5) How they compose

The modern layering, and where the JD's tools sit:

```
sources ──CDC/ingest──▶ raw / bronze          (Kafka, Debezium/DMS → S3 Parquet / Iceberg)
                    ──▶ integration layer     (Data Vault, or staging + conformed dims)      dbt models
                    ──▶ marts / silver-gold   (star schemas per domain: risk, lending ops, finance)  dbt models + tests
                    ──▶ serving               (OBT in ClickHouse / Pinot; feature tables; API reports)
```

- **Medallion** (bronze/silver/gold) is the same idea by other names; vault is one option
  for silver.
- **dbt** is the natural home for vault → star → OBT: each layer is a set of models with
  `ref()` lineage and tests (uniqueness on hub keys, not-null on grains, accepted values on
  SCD flags). Incremental models with the `merge` strategy for SCD2 and for OBT partitions.
- **Iceberg** holds raw / vault / star (transactional upserts, time travel for "what did
  the mart look like on the 3rd"). **ClickHouse / Pinot** hold OBT.
- **Multi-tenancy** rides through every layer as a key: `lender_id` on every hub, link,
  fact and OBT; partition-level isolation where a bank requires it.

## 6) A lending-domain model, worked

Reason with this in the room; adapt to what they tell you about the estate. Entities are
inferred from FinBox's products (BankConnect, DeviceConnect, AA, Sentinel decisions, Prism
partner lending) — confirm, don't assert.

**Business keys / hubs:** `borrower` (hashed PAN / mobile — never raw PII in the hub),
`application`, `loan`, `bank_account`, `lender` (tenant), `partner` (distribution channel),
`product`, `device`.

**Links:** `application ↔ borrower ↔ lender ↔ product ↔ partner` (the origination link),
`borrower ↔ bank_account`, `borrower ↔ device`, `loan ↔ application`.

**Satellites, one per source:** `sat_borrower_kyc` (KYC provider), `sat_borrower_bureau`,
`sat_bank_account_statement_summary` (BankConnect: income detection, obligations, fraud
flags — versioned per statement pull), `sat_device_signals` (DeviceConnect — consent-scoped),
`sat_application_decision` (Sentinel: rule version, score, outcome, reasons).

**Star marts on top:**

| Mart | Fact / grain | Dims |
|---|---|---|
| Origination | `fact_application` — one row per application; accumulating snapshot with stage timestamps | borrower (SCD2), lender, product, partner, date (role-playing), decision |
| Transactions | `fact_bank_txn` — one row per parsed statement line | account, borrower, merchant category, date |
| Portfolio | `fact_loan_balance_daily` — periodic snapshot per loan per day | loan, lender, product, date |
| Decisioning | `fact_decision` — one row per rule-engine evaluation | rule version, application, lender |

**OBTs for serving:** `obt_application_360` (one row per application, every attribute and
stage as-of decision time — the lender-facing report and the audit answer), `obt_borrower_features_daily`
(one row per borrower per as-of date — model features; as-of semantics are non-negotiable
here or you leak the future into training), `obt_lender_portfolio_daily` (dashboard).

**Where the sharp edges are** — bring these up yourself:

- **Consent scope.** AA and device data are purpose-limited; model them in their own
  satellites with a consent reference, so a revoked consent is a satellite-level deletion or
  crypto-shred, not a hunt across marts. OBTs derived from them are rebuilt.
- **Identity resolution.** The same human arrives via different partners with different
  identifiers; the hub is the *resolved* key and the resolution rule lives in the business
  vault, versioned — because a lender will ask why two applications were linked.
- **Tenancy.** `lender_id` on everything; OBTs partitioned by lender; a bank that needs
  physical isolation gets its own partition/bucket/account, and the model doesn't change.
- **Decision reproducibility.** `fact_decision` must carry the rule/model version and the
  satellite `load_date`s it read — that is how you re-run a decision for an auditor.

## 7) Questions an HM will ask

| They ask | You say |
|---|---|
| "Star or Data Vault for us?" | "Depends on two things: how many sources describe the same borrower, and how hard the audit requirement is. Hundreds of partner integrations and banks as customers says vault for the integration layer — but I'd keep it raw and thin, and put stars on top for analysis and OBTs for serving. If it turns out the sources are few and controlled, a star with SCD2 gets 90% of it at a third of the joins. I'd want to see the source list before committing." |
| "What's the grain of the application fact?" | One row per application; accumulating snapshot — a timestamp per stage, updated as it advances. Decisions are their own fact because an application can be evaluated more than once. |
| "How do you handle a borrower's attributes changing?" | SCD2 on `dim_borrower`, and the fact carries the surrogate key of the version current at event time — so reports are as-of by construction. In the vault, it's just another satellite row; the mart applies the as-of rule. |
| "Late-arriving data?" | Facts: as-of join on `valid_from/valid_to`. Dims: placeholder row, backfill. In a vault, late is normal — `load_date` vs event date are separate columns and PIT tables handle it. |
| "Where does multi-tenancy live in the model?" | On every key, at every layer, and as the first partition column on anything served. Isolation level is a per-customer decision, not a model decision. |
| "Deletion / DPDP in an append-only lake?" | Per-source satellites with a consent reference; delete or crypto-shred the satellite; Iceberg row-level delete + compaction to make it physical; rebuild derived OBTs. Time-travel snapshots have to expire too — retention on snapshots is part of the deletion story. |
| "OBT vs star for the dashboards?" | OBT in the serving store, star behind it. Never let the OBT become the truth. |
| "Data Vault — have you built one?" | "No — I've modelled financial facts as star-shaped ledgers and served OBT from Pinot. I know the vault mechanics and, more usefully, when it earns its joins. If your source count is what I think it is, I'd propose a thin raw vault and I'd want someone who's run one at scale to review the satellite-splitting rules." Honest, specific, and it names the risk. |

## 8) Map to what you've built

One sentence each, so the JD line reads as experience rather than reading:

- **Star:** the revenue-recognition ledger — journal entries → ledger → recognition entries
  → period summaries is a transaction fact plus a periodic-snapshot fact (per period), with
  merchant, product, contract, and date dimensions; grain discipline was the whole
  correctness story ([worked example](../revenue_recognition_walkthrough.md)).
- **SCD2 mechanics:** Iceberg `MERGE INTO` with change detection is what the incremental
  materialisation framework did — the same merge you'd write for a Type 2 dimension.
- **OBT:** every Pinot serving table was OBT by design — one grain per table, attributes
  as-of, rebuilt incrementally by partition from Iceberg.
- **Multi-tenancy in the model:** 10K+ merchants on reconciliation with per-merchant config
  — tenant key on every fact and every rule.
- **Vault:** conceptual — and say so, with the judgement in [§7](#7-questions-an-hm-will-ask).

## 9) Vocabulary to have cold

grain · conformed dimension · degenerate dimension · junk dimension · role-playing dimension ·
bridge table · factless fact · transaction / periodic snapshot / accumulating snapshot fact ·
additive / semi-additive / non-additive measure · surrogate vs natural key · SCD types 1/2/3
(and 6) · as-of join · late-arriving fact / dimension · hub / link / satellite · hash key ·
`hash_diff` · `load_date` / `record_source` · raw vault / business vault · PIT table ·
bridge (vault sense) · OBT · denormalisation · medallion (bronze/silver/gold) · `ref()` /
incremental model / `merge` strategy (dbt).

## Sources

- Kimball, *The Data Warehouse Toolkit* (star schema, fact types, SCD, conformed dims)
- Linstedt & Olschimke, *Building a Scalable Data Warehouse with Data Vault 2.0* (hubs/links/satellites, hash keys, PIT/bridge, raw vs business vault)
- [FinBox HM round](../finbox/01-hm-round.md) — the JD line this answers; [FinBox product context](../finbox/01-hm-round.md#b-finbox-in-the-terms-the-conversation-will-use) for the domain entities
- [Lead DE quick notes](quick-notes.md) — modelling checklist line this expands

*Prepared 2026-09-22.*
