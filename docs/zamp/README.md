# Zamp — Index

Company-specific interview prep for Zamp (`zamp.ai`) — an agentic operating system for
finance and back-office work. Not part of the HLD or LLD problem sets; kept here because
the material is about one company's round rather than one design problem.

| # | Doc | Covers |
|---:|---|---|
| 1 | [R2 — 90-minute System Design Round](01-r2-system-design-round.md) | format, company, interviewers, likely briefs, cross-cutting themes |
| 2 | [CEO / Behavioral Round](02-behavioral-round.md) | what "spikes" means, the career arc, story bank, worked answers, gaps to close |

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

### 2. [CEO / Behavioral Round](02-behavioral-round.md)

One round, with the CEO. HR's brief: *not day-to-day work — e.g. "improved latency 10%" — it
will be looking for **spikes**, in a conversation around the journey.* So competence is not
what is being measured: [what "spikes" means](02-behavioral-round.md#b-what-spikes-means) and
the [four-property test](02-behavioral-round.md#the-test--four-properties) a story has to pass,
over the top of the [Hello Interview](https://www.hellointerview.com/learn/behavioral/course/why-the-behavioral-matters)
Decode → Select → Deliver cycle (CARL, not STAR) and the
[five screens](02-behavioral-round.md#a-what-is-being-screened) Zamp's 13 values collapse into.

The [career arc](02-behavioral-round.md#d-the-career-arc--the-spine-of-the-hour) is the spine
of the hour — told as inflection points rather than a chronology, including the Recko move as
the startup-fit proof the prep was missing, and where the line goes flat. Then a
[six-story bank](02-behavioral-round.md#e-story-bank) marked for which ones are actually spikes,
and worked deliveries for [the spike question](02-behavioral-round.md#f-the-spike-question),
[why leaving Stripe](02-behavioral-round.md#g-why-are-you-leaving-stripe),
[leaving without an offer](02-behavioral-round.md#h-why-did-you-leave-without-another-offer-lined-up),
[conflict](02-behavioral-round.md#i-tell-me-about-a-time-you-faced-conflict),
[a problem you identified](02-behavioral-round.md#j-what-problem-did-you-identify) and
[why Zamp](02-behavioral-round.md#k-what-are-you-looking-for-in-your-next-job--why-zamp) —
each with its decode, follow-up table and the phrases to avoid. Closes with
[the founder-only questions](02-behavioral-round.md#l-the-other-founder-questions) — exceptional
at / bad at, the contrarian belief, why not found something —
[questions to ask](02-behavioral-round.md#m-questions-to-ask-them), and the
[gaps to close first](02-behavioral-round.md#n-gaps-to-close-before-the-round).

## Related

- [HLD interview framework](../hld/00-interview-framework.md)
- [HLD problems](../hld/README.md) · [LLD problems](../lld/README.md)
- [Revenue Recognition](../revenue_recognition_pipeline.md) — the closest existing doc to Zamp's domain
