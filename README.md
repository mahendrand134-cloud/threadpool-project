# High-Performance Thread Pool with Backpressure

## Overview
This project implements a custom thread-pool executor with:
- priority-aware task queue,
- configurable backpressure (accept / throttle / reject),
- non-blocking submit API that returns `CompletableFuture<T>`,
- simple worker lifecycle and graceful shutdown.

## Design highlights
- **Task prioritization**: `PriorityTask` enforces ordering in `PriorityBlockingQueue`.
- **Backpressure controller**: `BackpressureController` evaluates `queueSize` vs configured `maxQueueSize` and returns `ACCEPT`, `THROTTLE`, or `REJECT`.
- **Futures**: Submitting via `submit(Callable<T>, int priority)` returns a `CompletableFuture<T>`; rejected submissions complete futures exceptionally with `RejectedExecutionException`.
- **Concurrency primitives**:
  - `PriorityBlockingQueue` (thread-safe queue)
  - volatile flags for worker shutdown
  - `CompletableFuture` for non-blocking result retrieval

## Files
- `src/main/java/com/threadpool/`:
  - `PriorityTask.java` — priority base
  - `TaskQueue.java` — wrapper over `PriorityBlockingQueue`
  - `BackpressureController.java` — decision logic
  - `BackpressureDecision.java`
  - `Worker.java` — consumer thread
  - `ThreadPoolExecutorCore.java` — main API, submit + shutdown
  - `FutureTaskWrapper.java` — wraps callable into a `PriorityTask` and `CompletableFuture`
  - `MainTestProgram.java` — demo runner
- `src/test/java/com/threadpool/ThreadPoolTests.java` — JUnit tests

## How to build & run
1. Java 11+ recommended (works with your JDK 25).
2. (Optional) Install Maven and run:
   ```bash
   mvn test
