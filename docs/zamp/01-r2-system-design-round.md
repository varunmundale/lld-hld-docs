# 1. Zamp R2 — 90-minute System Design Round

[← Zamp index](README.md) · [All docs](../README.md)

---

Research notes for the Zamp (`zamp.ai`) R2 engineering round, assembled from the
[candidate guide](https://app.notion.com/p/zampfinance/R2-Engineering-Candidate-Guide-3c8edc94e67f803b9c33deeef49d3709),
the company's product pages, and the two interviewers' backgrounds. See
[sources](#sources) at the bottom.

## A) The format

It is **not** two rounds. One 90-minute continuous design where a single ambiguous brief
is taken HLD → LLD → stress test:

| Phase | Duration | Activity |
|---|---|---|
| 1 | 10 min | Discuss the brief, identify users and use cases, agree on scope |
| 2 | 25 min | Baseline HLD — primary request and data flows |
| 3 | **30 min** | Go deep on the critical component at LLD level |
| 4 | 25 min | Scale, latency, reliability, consistency, failure handling, trade-offs |

Remaining time is for your questions. The interviewer may reorder or dwell on one area.

Two things follow from the table:

- **The LLD block is the single largest.** Half the round is HLD; the deep dive is where
  the round is won. Pick the critical component early and deliberately — the guide says
  *"Choose one critical path for LLD."*
- **You do not get to recite an architecture.** The guide states you are not expected to
  reproduce any particular system, and that they are grading *"how you respond when the
  constraints become more demanding."* Phase 4 exists to move the goalposts.

### What they said they grade

Requirement gathering · assumptions and prioritization · HLD quality · LLD depth ·
scalability and performance · reliability and correctness · **extensibility and
maintainability** · trade-offs and judgment · communication.

Extensibility is called out explicitly — *"new rules, workflows, tenants, or capabilities
without requiring unsafe rewrites."* That is the same axis as the GoF/SOLID line in their
backend JD, and it is the one most candidates skip.

### What they want left on the board

Core use case + assumptions · a clear HLD with components and data flows · the important
APIs and entities · one meaningful LLD deep dive · behaviour at the stated scale · how
changes, failures, retries and operational events are handled · main trade-offs and next
improvements.

## B) The company, in the terms the design will use

Zamp is an agentic operating system for finance and back-office work — AI agents that
learn a finance process and run it end to end. Customers are Fortune 500s and global
banks, which makes multi-tenancy, auditability and encryption first-class, not garnish.

Product surface, as advertised:

| Area | What it covers |
|---|---|
| Reconciliation | bank, credit/corporate card, vendor statement (AP), customer (AR), intercompany, balance-sheet (prepaids, accruals, fixed assets, payroll clearing), payment processor, revenue, subledger → GL |
| Accounts payable | invoice intake → intelligent document processing → ERP/procurement action → exception reasoning → learning from prior resolutions |
| Treasury & close | forecasting, close reconciliations, compliance monitoring, vendor research |

Their own framing of the audit problem is worth borrowing verbatim in the round:
auditors do not object to automation, they object to automation that cannot show its work.

**Stack**, from the backend JD: Go, Redis, Kafka/Pub-Sub, Kubernetes, job schedulers —
and the JD names *Gang of Four principles, SOLID, and DRY* outright. Expect the Phase 3
deep dive to want real interfaces and a class model, not just a table of columns.

**Integration surface**, from the marketing site: ERP, inbox, spreadsheets, and
browser-based portals. Note the last one — some connectors are screen-scrapers, not APIs.

## C) The interviewers

### Nipun Agarwal — Founding Engineer (Jul 2022 → present)

Effectively engineer #1; four years in. Before Zamp: SWE at BrowserStack, and an **SRE
internship at CRED** where he built an in-house IAM/access-automation tool, optimised
CI/CD, built the monitoring and alerting framework for microservices ("reduced incidents
by 30%"), and architected an automated incident-response system. Earlier, at MIT Media
Lab, real-time collaborative editing over websockets scaled horizontally to 200k users.

→ **He owns Phase 4.** Expect multi-tenancy and tenant isolation, RBAC and access control,
idempotency, retry semantics, blast radius, what alerts fire and who gets paged. His
background is operational, so "how do you know it broke" is a live question, not a
formality.

### Kenan Collaco — Founding Team / Founding AI Engineer (Aug 2024 → present)

Before Zamp: Head of Technology at Bluelearn (250k-member student community, Lightspeed
and Elevation backed), developer at Numadic (FASTag and logistics payments), and
co-founder of a chatbot/WhatsApp-automation startup — which he publicly writes up as
failed to scale, a candid-postmortem sort of engineer.

→ **Product-shaped and agent-shaped.** Expect the Phase 1 ambiguity pressure ("who is the
user, what are we actually doing for them") and the LLM-in-the-loop questions — confidence
thresholds, when the agent escalates, how it learns from a correction.

## D) Most likely briefs, ranked

### 1. Reconciliation engine — highest probability

> *"A customer uploads bank statements and we pull ledger entries from their ERP. Design a
> system that matches them, surfaces exceptions, and satisfies an auditor."*

It is the flagship product and it is a genuinely good 90-minute problem.

**Phase 3 deep dive is the matcher.** What to have ready:

- **Candidate generation** — you cannot compare every statement line against every ledger
  entry. Block by amount bucket, date window, counterparty, currency. This is the
  scalability answer hiding inside the LLD answer.
- **Match strategies, pluggable** — exact, 1:1 with tolerance, 1:N (one payment settling
  many invoices), N:1 (batched deposit), N:M, fuzzy on amount tolerance and date drift and
  description similarity. Strategy pattern, ordered, deterministic rules exhausted before
  anything probabilistic runs.
- **The match as an entity with a state machine** — `proposed → confirmed | rejected`,
  plus `unmatched → escalated → resolved`. Who can transition, and what it writes.
- **Idempotency on re-ingest** — the same statement uploaded twice must not create
  duplicate proposals. Content hash per line + a natural key.
- **Append-only decision log** — every match, every rejection, every rule version that
  produced it. Replayable.
- **The Phase 4 follow-up to expect:** *"a matching rule changed — restate last quarter."*
  Rule versioning, and whether historical matches are immutable or recomputed.

Closest existing notes: [Rule Engine](../lld/15-rule-engine.md),
[Order Management System](../lld/17-order-management-system.md),
[Payment System](../hld/29-payment-system.md),
[Revenue Recognition](../revenue_recognition_pipeline.md).

### 2. Invoice-to-pay / AP automation

> *"Invoices arrive by email and through vendor portals. Get them from inbox to approved
> payment."*

Deep dive lands on the **approval workflow engine**: state machine, three-way match
(PO / GRN / invoice), duplicate-invoice detection across formats and vendors, extraction
confidence thresholds routing to a human queue, and the hard constraint — *never double
pay*. Idempotency here is money, not hygiene.

### 3. Agent task-orchestration platform — Kenan's likely angle

> *"An agent learns a finance process and runs it end to end. Design the execution
> platform."*

Deep dive is **durable execution**: step DAG persisted, retry with backoff, idempotency
key per side-effecting tool call, steps that suspend for days awaiting human approval,
tool/connector abstraction, compensating actions on failure. Effectively "build Temporal,
and then justify why you didn't just buy Temporal" — have the buy-vs-build answer ready.

Closest existing notes: [Task Scheduler](../lld/01-task-scheduler.md),
[Job Scheduler](../hld/18-job-scheduler.md),
[Task Execution Engine](../appendix/lld.md#b-task-execution-engine).

### 4. Connector / ingestion framework

> *"Onboard 200 banks and 5 ERPs, each with different auth, formats and rate limits."*

Deep dive: connector interface plus a registry, incremental sync with cursors and
watermarks, backfill versus live path, normalisation to a canonical schema, per-tenant
rate limiting, secret storage and rotation. The browser-portal connectors break the clean
abstraction — say so rather than pretending everything is a REST API.

### 5. Financial close / ledger and revenue recognition

Already covered end-to-end in [Revenue Recognition](../revenue_recognition_pipeline.md)
and its [worked example](../revenue_recognition_walkthrough.md) — the out-of-order events
and recovery sections transfer directly.

## E) Cross-cutting themes — they surface regardless of brief

| Theme | What to say |
|---|---|
| **Multi-tenancy** | Fortune 500s and banks. Tenant isolation, noisy-neighbour containment, per-tenant quotas, encryption at rest per tenant, data residency. Nipun goes here. |
| **Idempotency and exactly-once *effects*** | Money moves. At-least-once delivery plus idempotent effects — never claim exactly-once delivery. Idempotency key on every side-effecting call. |
| **Correctness over availability** | In finance you stall rather than double-pay. Say it out loud — it is the single trade-off that signals domain sense, and it inverts the usual interview reflex. |
| **Immutable audit trail** | Event-sourced decisions, replayable, with the rule version that produced each one. Their own words: automation that cannot show its work is the objection. |
| **Human-in-the-loop** | Confidence thresholds, escalation queue, SLA on the queue, and the feedback loop that folds a correction back into the model. |
| **Non-determinism vs auditability** | How do you make an LLM agent's decision reproducible six months later? Pin model + prompt + tool versions, log inputs and outputs, keep the deterministic rules as the audited path and the model as a proposer. Raising this unprompted is a differentiator. |
| **Extensibility** | *"Add a new reconciliation type / new bank / new rule without a redeploy."* This is the SOLID question in disguise, and it is on their published rubric. |

## F) Running the 90 minutes

Their guide is unusually prescriptive; treat it as the rubric it is.

1. **Restate the outcome** in terms of what a finance team accomplishes, not what the
   system does.
2. **Ask targeted questions** — actors, request types, read vs write path, traffic,
   latency, availability, consistency, security. Then stop asking and start designing.
3. **State assumptions out loud** whenever a requirement is missing, and label them as
   assumptions.
4. **Set scope** — name the v1 you are designing and explicitly park the rest.
5. **Simple baseline first.** End-to-end flow before any optimisation.
6. **Make the data model concrete** — entities, relationships, identifiers, versions,
   ownership boundaries.
7. **Pick the critical path and go deep** — interfaces, validation, execution steps, state
   transitions, error behaviour, enough that it is implementable.
8. **Explain trade-offs** — why this, what it costs, when you would choose otherwise.
9. **Leave time for questions.**

Communication notes from the guide, paraphrased: narrate rather than draw silently;
separate requirements from assumptions from choices from open questions; call out where
correctness, freshness, availability and latency are in tension; adapt the design in
response to follow-ups instead of defending v1; prefer one coherent design with stated
limits over a pile of technologies.

## G) Prep gap

There is no reconciliation / matching-engine write-up in this repo, and it is the single
most likely brief. That is the highest-value doc to add before the round — strategy
pattern over match rules, match state machine, tolerance windows, N:M matching, append-only
audit log.

Everything else maps onto existing material:

| Brief | Existing notes |
|---|---|
| Reconciliation engine | [Rule Engine](../lld/15-rule-engine.md) · [OMS](../lld/17-order-management-system.md) · [Payment System](../hld/29-payment-system.md) |
| Invoice-to-pay | [OMS](../lld/17-order-management-system.md) · [Payment Wallet](../lld/03-payment-wallet.md) |
| Agent orchestration | [Task Scheduler](../lld/01-task-scheduler.md) · [Job Scheduler](../hld/18-job-scheduler.md) · [Task Execution Engine](../appendix/lld.md#b-task-execution-engine) |
| Connector framework | [Rate Limiter](../lld/13-rate-limiter-full-design.md) · [Web Crawler](../hld/26-web-crawler.md) |
| Close / rev rec | [Revenue Recognition](../revenue_recognition_pipeline.md) · [worked example](../revenue_recognition_walkthrough.md) |
| Phase 4, any brief | [Scaling strategies](../appendix/scaling-strategies.md) · [Database decision list](../appendix/database-decision-list.md) · [Isolation levels](../appendix/isolation-levels.md) |

## Sources

- [R2 Engineering — Candidate Guide](https://app.notion.com/p/zampfinance/R2-Engineering-Candidate-Guide-3c8edc94e67f803b9c33deeef49d3709) — format, phases, evaluation criteria
- [zamp.ai](https://www.zamp.ai/) — positioning, integration surface
- [Automated Reconciliation: The Finance Team's Guide](https://www.zamp.ai/blogs/automated-reconciliation-the-finance-teams-guide) — reconciliation types
- [AI Agents for Accounts Payable](https://www.zamp.ai/blogs/ai-agents-for-accounts-payable) — AP flow
- [Backend Engineer JD](https://builtin.com/job/backend-engineer/2540825) — stack, GoF/SOLID/DRY
- [Kenan Collaco](https://www.linkedin.com/in/kenancollaco/) · [Nipun Agarwal](https://www.linkedin.com/in/nipunagarwal99/)

*Researched 2026-09-04.*
