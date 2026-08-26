# 35. RAG Application with Ingestion Pipeline

[← HLD index](README.md) · [All docs](../README.md)

---

## Reference design

<sub>Worked submission — [hellointerview.com/community/submissions/system-design/cmmwinscm06mb0fadf371kg4t](https://www.hellointerview.com/community/submissions/system-design/cmmwinscm06mb0fadf371kg4t) (external link; no Excalidraw board for this one)</sub>

- Training documents store
- Model training
- Given a query, answer specifically — knowledge AI

## Requirements
1. API to upload documents + update
2. Model trains on every document
3. User queries → answer given
4. Based on user feedback, retrain model

## NFR
1. Response ≥ 1s
2. Availability, eventual consistent
3. Cost control checks?

## Steps (Ingestion)
1. Extraction — parse CSV, PDF, Wiki
2. Chunking — convert to specific size limit
3. Create embedding for each chunk & store
4. Create ES index

## Steps (Query)
1. Do an embedding search / text search on docs
2. Retrieve relevant chunks (ES returns this)
3. LLM reads & generates answer
   → Add in the context window

## Summary
- Store + (Temporal)
- Multistep: parse + chunk + embed + index
- Metadata: doc status
- Query Service: RAG — retrieve + augment + generate
