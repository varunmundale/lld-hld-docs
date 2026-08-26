# 2. Publisher-Subscriber Model

[← LLD index](README.md) · [All docs](../README.md)

---

## Functional requirements
1. Write to a topic
2. Read from a topic

## NFR
1. Guarantees:
   - (a) At-least — focus on this
   - (b) Exactly
   - (c) At-most
   - Duplicate handling?
2. Scalability / Load
3. Thread safe
4. Ordering
5. Retries (DLQ)
6. Backpressure

## Core Components
- Message (id, message)
- Queue (id)
- write(message, topic)
- read(topic)

Observer pattern can be used
- Notify when message is produced to all the consumers subscribed to the topic

- Initializations (Object lifecycle)
- Application DTOs
- DB — classes, interfaces
