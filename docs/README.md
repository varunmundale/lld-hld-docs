# System Design Notes

Transcribed from handwritten Xournal++ notebooks, one file per problem, with the
original whiteboard diagrams rendered alongside.

| | | |
|---|---|---|
| **[HLD problems](hld/README.md)** | 38 problems + the [interview framework](hld/00-interview-framework.md) | 16 with whiteboard diagrams |
| **[Requirements index](hld/requirements-index.md)** | Ideal FR / NFR / out-of-scope for all 39 problems | plus the **crux** of each and what clears the bar at mid / senior / staff+ |
| **[LLD problems](lld/README.md)** | 20 problems | 9 with worked class diagrams |
| **[Appendix](appendix/README.md)** | [HLD](appendix/hld.md) — consistency models, Elasticsearch, Cassandra, Kafka, Redis, Iceberg, Pinot | [LLD](appendix/lld.md) — concurrency, task execution engine |
| **[Playbooks](appendix/README.md#playbooks)** | [Scaling strategies](appendix/scaling-strategies.md) — caching, sharding, load balancing | [Database decision list](appendix/database-decision-list.md) — which store, and which keys |
| **[Diagrams](diagrams/README.md)** | 19 renders + editable `.excalidraw` sources | |
| **[LeetCode](leetcode/README.md)** | [Quick index](leetcode/index.md) — 198 solved by pattern, attempts, ⚠️ re-drill list | [Quick notes](leetcode/notes.md) — crux + pitfalls/traps/edge cases per problem, recurring-traps table, handwritten notes folded in |
| **[Data engineering](data-engineering/README.md)** | [Lead DE quick notes](data-engineering/quick-notes.md) — fundamentals checklist, sessionization, deterministic ids, RAND/salting, DE-style LLD | [Data modelling](data-engineering/data-modelling.md) — star, Data Vault 2.0, OBT, worked lending model |
| **[Zamp](zamp/README.md)** | [R2 system design round](zamp/01-r2-system-design-round.md) — format, likely briefs, interviewers | [CEO / behavioral round](zamp/02-behavioral-round.md) — "spikes", the career arc, worked answers |
| **[FinBox](finbox/README.md)** | [Hiring manager round](finbox/01-hm-round.md) — Lead DE: JD → evidence map, why leaving / why FinBox, gap questions, first 90 days | |
| **[Résumé](resume/README.md)** | [2026 PDF](resume/Varun_Mundale_2026.pdf) + markdown transcription — the canonical numbers | |
| **Code** | [Temporal SDK example](../src/main/java/org/example/temporal/README.md) — Uber ride dispatch, durable execution, runnable | [Pinot segment replace](../src/main/scala/com/revrec/pinot/README.md) — Spark job |

## Standalone designs

Not part of either problem set — kept on their own because there is no matching problem:

- [Revenue Recognition — end to end](revenue_recognition_pipeline.md) — Mongo oplog → Flink → SQS → Iceberg → Pinot / DataStory
  - [worked example](revenue_recognition_walkthrough.md) — a Netflix subscription through journal → ledger → revenue recognition entries → period summary
- [Database Version Control](database_version_control.md) — branch, diff, validate and commit a schema like git

## Layout

```
docs/
  hld/         one file per HLD problem, NN-slug.md   + README.md index
  lld/         one file per LLD problem, NN-slug.md   + README.md index
  appendix/    hld.md, lld.md, cheat sheets            + README.md index
  diagrams/    PNG renders, excalidraw/ scene sources  + README.md index
  zamp/        company-specific interview prep         + README.md index
  finbox/      company-specific interview prep         + README.md index
  resume/      current résumé, PDF + markdown          (README.md is the transcription)
  data-engineering/  DE interview prep + reference notes + README.md index
  leetcode/    solved-problem index + crux/pitfall notes + data/ (raw export)
```

Every diagram ships as a PNG plus its `.excalidraw` scene under
[`diagrams/excalidraw/`](diagrams/excalidraw) — drag one into
[excalidraw.com](https://excalidraw.com) to edit it. Note that the JetBrains Excalidraw
plugin re-wraps container labels with its own font metrics when it saves, which mangles
multi-line labels; check the diff before committing a scene it has touched.
