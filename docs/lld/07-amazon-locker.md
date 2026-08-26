# 7. Amazon Locker

[← LLD index](README.md) · [All docs](../README.md)

---

*(numbered "2)" — new session, restart numbering)*

Driver → [box] ← Customer

**Driver**
1. Deposits a package into available compartment
   - (a) Single or multiple
   - (b) Availability — system or human
   - (c) How system selects slot?
   - (d) Slot sizes

**Flow**
1. Customer places order (package)
2. Driver delivers order in compartment
   - (a) Slot is selected
   - (b) Driver places [package]
   - (c) Access code generated, notified (SMS, Email)
3. Customer retrieves package by:
   1. Selecting slot
   2. Entering access code

## Clarify — Edge cases
- (e) Access code expiry?
- (f) 3 times fail
- (g) What if compartments full

## Requirements
1. Driver deposits a package by size (S, M, L)
   - Driver enters size
   - System matches a slot (size)
   - Opens compartment
   - Driver deposits package & closes compartment
   - System generates access code (TTL)
   - Access code sent to customer
2. User retrieves package by entering access code
   - User enters access code
   - Validates
   - Throw exception if code expired, invalid
3. System
   - Monitors & expires code

## Out of scope
- Whether valid package stored (assume it is)
- UI
- Notification system
- Driver access control
- Order management system

## Entities
- Driver (name, id)
- User (name, id)
- Locker
  - Compartment[]
- Compartment
  - isFree
  - size {S, M, L}
  - AccessToken
- AccessToken (code, expiration, package)

## More Clarifying questions + Extensions + Optimizations
1. Is access token a bearer token?
2. Do we need to handle OMS?
3. Address: not handling — 2-phase approach: open → deposit/pickup → close

1. Open expired compartments()
2. Index by accessCode for fast access
3. Index by compartment size + isFree for fast access
4. Non-add-size fallback — change allocation strategy
5. Compartments can break — add state OUT_OF_SERVICE
6. Ensure package is deposited — confirmDeposit API (which can be called via sensor), prompt deposit
