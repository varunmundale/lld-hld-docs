# 3. Payment Wallet (LLD)

[← LLD index](README.md) · [All docs](../README.md)

---

## Functional Requirements
1. Add to wallet
2. Spend from wallet
3. Transaction history
4. Wallet-wallet transfer
5. Multicurrency support?

## NFR
1. Transactional guarantees
   - Atomicity of transactions (concurrency, thread safe)
   - Immutability
   - Retries on failure

## Core Entities
- User
- Wallet (Account)
- TransactionStatus
- Transaction

Saga vs 2PC
Idempotency key
