# LLD Appendix — Notes

*Transcribed from handwritten Xournal++ notebook (`lld_appendix.xopp`), 12 pages.*

## Table of Contents
- [A) Concurrency](#a-concurrency)
  - Reference table: Correctness / Coordination / Scarcity
  - Coarse-grained vs fine-grained locking
  - Read-write locks, atomic variables, common bugs
  - Producer-Consumer problems
  - Blocking queues, message passing (Actor model)
- [B) Task Execution Engine](#b-task-execution-engine)
  - DAG-based task orchestration
  - Naive (per-task lock/wait) approach
  - Better approach (remainingDeps counter)

---

## A) Concurrency

**Eg:**
- 2 users book same flight
- 3 threads update same counter
- Dozen requests hit cache (while write in progress)

**LLD**
- Thread pools
- Rate limiters
- Connection pools
- Schedulers

**Interviewer checks:** what breaks + what to fix?

**Thread** = PC + registers + stack + heap*. Unpredictability in switching.
Atomic at source: `i++` ✗ atomic

**Atomics:** `java.util.concurrent.AtomicInteger` — counter, `incrementAndGet()`

**Lock:** concurrent locks, `ReentrantLock` (Mutex)
```
synchronized(lock) {
  = ...
}
```
equivalent to:
```
try {
  lock.lock()
  = ...
} finally {
  lock.unlock()
}
```

Lock also can wait! — condition variables

*(*heap is shared across threads; PC, registers, and stack are per-thread)*

**Semaphore:** `java.util.concurrent.Semaphore`
```
s = new Semaphore(5);
s.acquire()  // ×5 successful
s.release();
```
Used to **limit** concurrent operations.

**Condition variables:** efficient locks
```
synchronized(lock) { // thread must own object's monitor
  while (!condition) {
    lock.wait();  // release lock & wait
  }
}
```

**Blocking Queues:** concurrent — `LinkedBlockingQueue`
```
LinkedBlockingQueue(100)
queue.put(task)
queue.take();
```

**ConcurrentHashMap:** atomic mutate key

### 3 problem types
1. **Correctness**: shared state — race & corruption
2. **Coordination**: hand-off work — Producer/Consumer
3. **Scarcity**: 10 DB connections, 100 requests

### Reference table (from embedded image)

| Problem Type | What Breaks | Solutions | Common Problems |
|---|---|---|---|
| **Correctness** | Shared state is updated concurrently | Locks, atomics, thread confinement | Check-then-act, read-modify-write |
| **Coordination** | Threads need ordering or handoff | Blocking queues, actors, event loops | Async request processing, bursty traffic |
| **Scarcity** | Resources are limited | Semaphores, resource pools | Concurrent op limits, resource consumption, object reuse |

### 1) Correctness
- Data corruption: multiple threads share access
- Eg: seat booking, counter → 847 instead of 1000 (missed writes)

**Bugs:**
1. Check-then-act
2. Read-modify-write

### Coarse-grained locking

**BookingService**
```
- lock object, Map seatOwners
synchronized(lock) {
  while (seatOwners not empty)
    return false;  // returning terminates the thread
  seatOwners.update
}
```

**Coarse grained** — CS (critical section) is short, moderate contention.

One lock for all seats — slow
Better: one lock per seat — fast

### Read-write locks
Workload is heavily skewed.
Typically used for cache — serialize the writes.

Expiry scenarios: RW locks used in ConcurrentHashMap (concurrent CHM)
`User → Role → Permissions`. Auth cache.

### Fine-grained lock
- Lock per seat: `Map seatOwners`
- **Locks**: `Map<seatId, Object>`

**Challenges:** Deadlock
- Multiple seat booking
- Lock in consistent ordering

### Atomic Variables
- CAS
- `incrementAndGet()`

CAS is MVCC, OCC — returns false if version changed.

### Common Bugs
1. **Check-then-act**
   Eg for rate limiter: use `sync() {}`

2. **Read-modify-write**
   - (a) Eg: `req++` → sol: Atomic Variable
   - (b) Eg: bank balance → sol: balance lock

Identify what data could get corrupted.

**Producer-Consumer** is classic sync problem with above bugs.
- The shared data is the Queue.

### Problems
1. Two producers update at same time & modify pointers (lost update)
2. Two consumers update at same time & modify pointers (read duplicate)
3. Race between `isEmpty()` & `remove` — can remove on empty queue!
4. Race between `isFull` & `add` — can add to full queue.

(iii) & (iv) are examples of check-and-act.

### 2) Coordination
- Async workload

1. **Naive approach**
   ```
   while (not queue empty)
     - remove 1st task & execute
   ```
   **Problem:** busy-waiting.

2. **Better**
   ```
   while (!q empty)
     - remove 1st task, execute & sleep
   ```
   **Problem:** latency in consumer. Eg: task arrives 1ms after worker sleeps.

**What if producer is faster?**
- Leads to consumer lag, OOM for unbounded queue (if in memory)

This is coordination problem.
1. Efficient waiting: co-ordinate
2. Backpressure: producer slow down
3. Thread safety: co-ordination must handle concurrent access

### Solution
1. Shared state coordination
2. Message passing coordination

`wait`, `notifyAll()`
Why `notifyAll()`? — JVM can wake up [any] thread. So, `while() {}`

### Blocking queue
- It is based on wait-notify. It is a bounded queue.
- When q is empty, consumer is blocked
- When q is full, producer is blocked
- `put()`: blocks producer (producer goes in wait)
- `offer()`: the producer request is rejected
- `offer(timeout)`: request is rejected with timeout
- `take()`: consumer goes in waiting (blocked)
- `poll()`: consumer goes in busy-waiting
- `poll(timeout)`: consumer goes in waiting after timeout

`LinkedBlockingQueue<Task>`

### Message Passing coordination
**Actor model**
Actor = Inbox + message handler (blocking queue)

### 3) Scarcity
- Few instances of resource, multiple requests
1. **Semaphore**: acquire semaphore of fixed size
2. **Resource Pooling**: maintain blocking queue with resource objects itself

Semaphore can also be used for quota management. Eg: cap 100MB RAM.

Use blocking queue, since we anyway need to create & assign correctly resource objects.

Use `poll(timeout)` to acquire connections.

---

## B) Task Execution Engine

### Requirements
1. Task has input dependencies
2. Tasks executed in parallel
3. Tasks have custom execution
4. Task DAG is valid
5. Task can fail; all downstream skipped
6. All dependencies can send/store output as input to downstream

**Task**: inputs[], id, output

`Map<id, Task> graph`
graph: `Map<id, Set<>>`
`Map<id, Task>` — TaskDB

**Orchestrator Thread**
- Get tasks with no dependencies. Execute.
- Every task blocked on inputs[]
- If task failed — mark all downstream as SKIPPED

For each task, create condition variables in consistent ordering (to avoid deadlock)

```
while (true) {
  forEach (inputI: inputsLocks)
    if (inputI == SUCCESS) {
      inputI.wait();
    } else if (inputI == failed)
      mark skipped & exit
  }
}
```

- Spawn thread for each task
- When task completes, `notifyAll()` downstream

For each task have a shared lock. Coordinate via `wait` & `notifyAll()`.

- Keep a shared DAG, keep a shared lock
- Spawn all tasks:
  - Get graph, `notifyAll()`
  - Get graph, input `wait()` on all

`ConcurrentHashMap<id, Task>`

**Task**: id, taskLock, status, customFunction

**Singleton** = DAG, CHM

**Orchestrator**
- Populate singleton
- Spawn all tasks

**Eg:**
```
Task1 → Task2
Task2 → Task3
Task2 → Task4
Task3 → Task6
Task3 → Task5
Task4 → Task5
```

**TaskManagement**: addTask, addDependency
**Task**: id (5), status, execute → dependencyGraph, TaskDB
`read(String[] lines)`

Main: readLines
Test: direct inject input

- Create locks for all tasks on input dependencies
- When upstream:
  1. Fails/skip — mark this task SKIP, return
  2. Not complete — `wait()` on input lock
  3. Complete — proceed

Put above things in while (tight loop) (avoid race)

Once this task completes, `notifyAll` downstream locks

### Better approach
For every task, maintain remainingDeps.

1. When task completes successfully:
   ```
   forEach downstream task(t)
     t.remDeps--
     if (t.remDeps == 0)
       submit DownstreamTask
   ```
   **Pro:** submit only when deps ready.

   (a) **Task**: id, status, function
   (b) **TaskExecEngine**:
       1. Static dependency graph — outgoing edges
       2. TaskDB

2. If task fails/skipped (mark all deps skipped):
   ```
   markSkipped()
     for d in downstream
       d.status = skipped
       d.markSkipped()
   ```

- Decrement should be **Atomic**
- `Map<String, Object> results` — CHM (ConcurrentHashMap)

---

*End of transcription.*
