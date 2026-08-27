# System Design Notes

Transcribed from handwritten Xournal++ notebooks, one file per problem, with the
original whiteboard diagrams rendered alongside.

| | | |
|---|---|---|
| **[HLD problems](hld/README.md)** | 33 problems + the [interview framework](hld/00-interview-framework.md) | 16 with whiteboard diagrams |
| **[LLD problems](lld/README.md)** | 16 problems | 5 with worked class diagrams |
| **[Appendix](appendix/README.md)** | [HLD](appendix/hld.md) — consistency models, Elasticsearch, Cassandra, Kafka, Redis, Iceberg, Pinot | [LLD](appendix/lld.md) — concurrency, task execution engine |
| **[Diagrams](diagrams/README.md)** | 19 renders + editable `.excalidraw` sources | |

## Standalone designs

Not part of either problem set — kept on their own because there is no matching problem:

- [Revenue Recognition — end to end](revenue_recognition_pipeline.md) — Mongo oplog → Flink → SQS → Iceberg → Pinot / DataStory
- [Database Version Control](database_version_control.md) — branch, diff, validate and commit a schema like git

## Layout

```
docs/
  hld/         one file per HLD problem, NN-slug.md   + README.md index
  lld/         one file per LLD problem, NN-slug.md   + README.md index
  appendix/    hld.md, lld.md, cheat sheets            + README.md index
  diagrams/    PNG renders, excalidraw/ scene sources  + README.md index
```

Every diagram ships as a PNG plus its `.excalidraw` scene under
[`diagrams/excalidraw/`](diagrams/excalidraw) — drag one into
[excalidraw.com](https://excalidraw.com) to edit it. Note that the JetBrains Excalidraw
plugin re-wraps container labels with its own font metrics when it saves, which mangles
multi-line labels; check the diff before committing a scene it has touched.
