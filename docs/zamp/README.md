# Zamp — Index

Company-specific interview prep for Zamp (`zamp.ai`) — an agentic operating system for
finance and back-office work. Not part of the HLD or LLD problem sets; kept here because
the material is about one company's round rather than one design problem.

| # | Doc | Covers |
|---:|---|---|
| 1 | [R2 — 90-minute System Design Round](01-r2-system-design-round.md) | format, company, interviewers, likely briefs, cross-cutting themes |

## Summary

### 1. [R2 — 90-minute System Design Round](01-r2-system-design-round.md)

One continuous 90-minute round, not two — a single ambiguous brief taken through
[scoping (10m)](01-r2-system-design-round.md#a-the-format), HLD (25m), a **30-minute LLD
deep dive on one critical component**, then 25 minutes of scale, reliability and
trade-offs. Covers [what Zamp builds](01-r2-system-design-round.md#b-the-company-in-the-terms-the-design-will-use)
in the vocabulary the design will need (reconciliation types, AP, close, treasury; Go /
Redis / Kafka / K8s; GoF and SOLID named in the JD), the
[two interviewers](01-r2-system-design-round.md#c-the-interviewers) and which phase each
is likely to own, [five ranked briefs](01-r2-system-design-round.md#d-most-likely-briefs-ranked)
led by a reconciliation engine, the
[themes that surface regardless of brief](01-r2-system-design-round.md#e-cross-cutting-themes--they-surface-regardless-of-brief)
— multi-tenancy, idempotency, correctness over availability, audit trail, human-in-the-loop,
LLM non-determinism vs auditability — and a
[map from each brief onto existing notes](01-r2-system-design-round.md#g-prep-gap).

## Related

- [HLD interview framework](../hld/00-interview-framework.md)
- [HLD problems](../hld/README.md) · [LLD problems](../lld/README.md)
- [Revenue Recognition](../revenue_recognition_pipeline.md) — the closest existing doc to Zamp's domain
