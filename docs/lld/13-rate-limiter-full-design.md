# 13. Rate Limiter (Full Design)

[← LLD index](README.md) · [All docs](../README.md)

---

*(numbered "8)")*

> Deployment-level view of the same problem: [Rate Limiter HLD diagram](../diagrams/rate-limiter.png) — see [HLD Problem 13](../hld/13-rate-limiter.md).

- How many client requests API can make (window)
- request → rateLimiter checks. If cap hit, request rejected.

## Scoping
1. Where is rate limiter located?
   `client → rateLimiter → API`. All calls go to rate limiter.
   `register(apiName)`, `check(apiName)` — (quota, window)
2. Multiple APIs
3. `step()` simulation? Or system time?
4. Multiple algo extensible? Templatize RateLimiter?
5. Static rules? What else parameter — user, API, device?
   `request, config` — config is heterogeneous. Return (status, message)? When to retry/reset?
6. No config?
7. Concurrency? ✗

## Flow
- Client calls RateLimiter (headers + algo, Request)
- RateLimiter is configured for APIs (config varies for different algos)
- RateLimiter accepts, redirects to actual API & returns response, else
- Reject request with (code, response)
- `step()` function — SystemService

## Out of scope
- Distributed
- Complex rules?

## Entities
- **Request**
- **RateLimiter**: config, (status, code), `process(Request)` → SlidingWindow
- **Config**: API
- **SlidingWindow** (maintain queue, clear expired)
- **SystemService**: `step()`
- **APIRateLimiter mapping**: `Map<API, RateLimiter>`

## Extensibility
1. How would you add new rate-limit algorithm?
   - Add it in the factory for each API
2. Dynamically handle config changes?
   - Update in factory object via put method
   - **Better**: expose `updateConfig` method for each limiter to **preserve** the ongoing state
3. How to handle concurrency?
   - ConcurrentHashMap — makes sure requests are added consistently in queue
   - Locks per limiterKey: to atomically execute check-and-set operations (in this case, cleanup + accept/reject request — check on limit)
4. Memory growth?
   - Extract out storage + lock — storage layer
