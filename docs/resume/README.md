# Résumé — 2026 (highlights + on-call + personal project)

[← All docs](../README.md)

Source: [`Varun_Mundale_2026.pdf`](Varun_Mundale_2026.pdf) — the version submitted for the
FinBox Lead Data Engineer application (Instahyre, 2026-09-17). Transcribed here so the
company prep docs ([FinBox](../finbox/README.md), [Zamp](../zamp/README.md)) can link to
specific bullets, and so the numbers the interviewer is holding are the numbers you say.

**Numbers on this résumé are canonical.** Where an older source disagrees — the Zamp prep
*and* the Google-Doc notes both say "5,000+ merchants"; this says "10K+" — the résumé wins
because the interviewer has it, so make sure the résumé number is the one you can defend.

Detail the PDF drops but the Google-Doc notes keep (say it if asked): Senior Software
Engineer (IC2) Sep 2021 → promoted to **Lead, Mar 2025**; Spark **3.3**, Iceberg **1.3**
(`MERGE INTO`), Airflow **2** — you led the Spark 3.3 / Airflow 2 migration; $600/day ≈
₹50,000/day; IIIT-H CGPA 7.94, GATE **454 / 108,495**.

---

**Varun Mundale** · Lead Software Engineer — Distributed Systems | Backend Engineering |
Financial Data Platforms · Bengaluru · varunmundale@gmail.com ·
[LinkedIn](https://www.linkedin.com/in/varun-mundale-6b8337a6/)

## Professional summary

Lead Software Engineer with 8+ years designing and building large-scale distributed
systems, backend services, and financial data platforms. Led end-to-end engineering
initiatives from architecture to production, with measurable improvements in latency, data
freshness and infrastructure efficiency. Event-driven architectures, stream processing and
financial systems, with a focus on correctness, reliability and operational excellence.

## Experience

### Stripe (via Recko acquisition) — Lead Software Engineer · Sep 2021 – Apr 2026 · Bengaluru

**Highlight:** 12x faster end-to-end financial reporting (12 h → < 1 h) · 10–20x lower
dashboard latency · reconciliation scaled to 10K+ merchants · incident classes caught
pre-production.

Led architecture and delivery across Stripe's **Revenue Recognition** and **Reconciliation**
platforms. Owned on-call and reliability for the platform, driving a shift from reactive
incident response to systematic pre-production prevention.

#### Incremental Reporting & Serving Platform · Mar 2025 – Apr 2026

- **Highlight:** 3x faster reporting (3 h → < 1 h) · P99 latency 10–20x lower (10–20 s → sub-second).
- Led architecture and implementation of the next-generation reporting platform, evaluating
  multiple architectural approaches to balance recoverability, data freshness, query latency
  and infrastructure cost.
- Designed an incremental report materialization framework on Iceberg transactional upserts,
  eliminating full-table recomputation.
- Integrated Apache Pinot as the analytical serving layer; designed replay and recovery
  workflows using Iceberg snapshots and time travel for reliable backfills.
- *Spark, Scala, Iceberg, Apache Pinot, Kafka, Airflow 2*

#### Incremental Processing Architecture · Sep 2023 – Feb 2025

- **Highlight:** 4x faster pipeline (12 h → 3 h) · $600/day cost reduction · org's critical
  reporting-freshness milestone unlocked.
- Redesigned the Revenue Recognition pipeline from whole-world batch to an incremental
  delta-based architecture, processing only changed financial entities while preserving
  accounting correctness.
- Decomposed monolithic Spark workflows into change-based processing; cross-functional
  validation of the architecture and downstream integration.
- *Spark, Scala, Airflow, Trino, Iceberg, MongoDB CDC, Flink*

#### Reconciliation Platform · Sep 2021 – Aug 2023

- **Highlight:** merchants scaled to 10K+ · post-acquisition platform build (Recko → Stripe).
- Led technical design and implementation of Stripe's reconciliation platform after the Recko
  acquisition — Alpha, Private Beta and Public Beta releases.
- Configurable reconciliation workflows: merchant opt-in/opt-out, date-based matching,
  compliance requirements; contributed to onboarding 10K+ Stripe merchants.
- *Spark, Scala, Airflow, Iceberg*

#### On-Call & Reliability · cross-cutting

- As on-call DRI, identified a broader systemic reliability problem rather than treating each
  production incident as isolated.
- Consolidated and categorised historical incidents by root cause and failure pattern
  (using Incidents AI) — surfaced recurring gaps: merchant onboarding, missing business-level
  validation, low staging fidelity, undocumented critical flows.
- Proposed and built a production-like staging subsystem — dual upstream modes (production
  and staging) and configurable input-data variants — so the team and dependent teams could
  replay incident-prone edge cases end-to-end. Reactive reliability → proactive prevention.
- Business invariant checkers over real staging output, converting production incident
  patterns into automated correctness checks; documented critical flows to speed up on-call
  debugging and on-callee onboarding.

### Flipkart — Software Engineer 2 · May 2019 – Aug 2021 · Bengaluru

**Highlight:** DRI · financial data platform made self-healing · Big Billion Day peak handled ·
regulatory compliance automated.

- **Data Correctness & Completeness:** as DRI, designed and built a platform to proactively
  detect, classify and automatically remediate accounting discrepancies in Flipkart's
  financial systems; unified observability for engineering, finance and product.
- **Big Billion Day readiness:** load and stress testing of critical accounting services,
  bottleneck identification and optimisation for peak.
- **Tax compliance framework:** automated testing framework for TDS/TCS regulatory changes.
- *Java, Python, Spark, Kafka, MongoDB, HBase, Locust*

### Tesco — Software Engineer 1 · Jul 2018 – May 2019 · Bengaluru

- Redesigned the queuing system for the Grocery Home Shopping (GHS) Orders API — reliable
  distribution of orders from the central OMS to retail store systems. *C# .NET, MySQL*

### Flydubai Airlines — Software Engineer Intern · Jun 2017 – Aug 2017 · Hyderabad

- ML system classifying email threads and call-centre summaries into 160 business
  categories. *Python, TensorFlow*
  *(PDF reads "June 2017 – August 2019" — a typo; it overlaps Tesco. Fix before the next send.)*

## Personal project

### dbgit — Git for database schemas · Jul 2026 – Aug 2026 · [github.com/varunmundale/database-version-control](https://github.com/varunmundale/database-version-control)

- Git-style branch / diff / merge for database schemas · team-safe · multi-dialect.
- Each branch is a real, private copy forked in seconds; schema changes can be tried,
  reviewed and thrown away without touching production.
- Diff and merge to column and index granularity, flag only genuine conflicts, rehearsed on a
  scratch database first; safe for concurrent users.
- *Java, jOOQ, JSqlParser, PostgreSQL, MySQL, H2, Docker* — design write-up:
  [Database Version Control](../database_version_control.md)

## Skills

Java · Scala · Spark · Python · Ruby · MySQL · Pinot · Trino · MongoDB · Hive · HBase · Kafka ·
Flink · Airflow · Jenkins · Metabase · Kubernetes · Docker · GitHub · IntelliJ · Claude

## Education

- **IIIT Hyderabad** — M.Tech, Computer Science & Engineering · 2016–2018
- **Mumbai University (FCRIT)** — B.E., Information Technology · 2013–2016
- **Mumbai University (VESP)** — Diploma, Information Technology · 2010–2013

## Known nits in the PDF

Fix these before the next submission — small, but a lead-level reader notices:

- Flydubai internship end date (see above).
- "by root cause **an** failure patterns" → "and".
- "Airflow2" → "Airflow 2".
- The Stripe title reads "Lead Software Engineer" for the whole tenure; promotion was Mar
  2025. Fine on a résumé, but be ready to say so if asked.
- **Missing bullet, worth adding under the reporting platform:** you onboarded and mentored
  four engineers into the team and planned the project; two of them became your execution
  partners. For a *Lead* application this is the one line the résumé lacks and the JD asks
  for ("mentor engineers and establish standards"). Draft: *"Planned and led the reporting
  platform delivery with a team of four engineers I onboarded and mentored; two grew into
  owners of the serving and materialisation layers."*
