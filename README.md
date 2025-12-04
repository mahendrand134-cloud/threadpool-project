High-Performance Thread Pool with Priority Scheduling & Backpressure

A custom Java thread-pool executor featuring:

Priority-aware task queue (high priority executes first)

Configurable Backpressure (ACCEPT / THROTTLE / REJECT)

Asynchronous task submission via CompletableFuture<T>

Dynamic worker scaling (min/max threads)

Worker idle timeout & shrinking

Graceful shutdown

JUnit test suite

 1. Features
 Priority Scheduling

Tasks are ordered using a PriorityBlockingQueue, where higher priority executes first.

 Backpressure Control

Automatically prevents overload using 3 modes:

Mode	Trigger	Behavior
ACCEPT	Queue low	Normal submit
THROTTLE	Queue > 75%	Artificial delay (50ms)
REJECT	Queue > max size	Task rejected
 Future-Based Submission
CompletableFuture<T> submit(Callable<T> callable, int priority);

 Dynamic Worker Management

minThreads always kept alive

maxThreads upper limit

Workers auto-shrink if idle for keepAliveMillis

 Graceful Shutdown

Safe worker termination with task completion guarantees.

 2. Project Structure
mahendrand134-cloud-threadpool-project/
│── README.md
│── LICENSE
│── pom.xml
└── src/
    ├── main/java/com/threadpool/
    │   ├── BackpressureController.java
    │   ├── BackpressureDecision.java
    │   ├── FutureTaskWrapper.java
    │   ├── PriorityTask.java
    │   ├── TaskQueue.java
    │   ├── ThreadPoolExecutorCore.java
    │   ├── Worker.java
    │   └── MainTestProgram.java
    └── test/java/com/threadpool/
        └── ThreadPoolTests.java

 3. Architecture Overview
 ┌───────────────┐     submit()     ┌─────────────────────────┐
 │   User Code    │ ───────────────▶ │ ThreadPoolExecutorCore  │
 └───────────────┘                   └─────────────┬───────────┘
                                                   │
                                                   ▼
                                     ┌──────────────────────────┐
                                     │  Priority Task Queue     │
                                     │  (PriorityBlockingQueue) │
                                     └─────────────┬────────────┘
                                                   │
                                                   ▼
                                  ┌────────────────────────────────┐
                                  │        Worker Threads          │
                                  │  run(), shrink, keepAlive      │
                                  └────────────────────────────────┘

 4. Usage Example
ThreadPoolExecutorCore pool = new ThreadPoolExecutorCore(2, 4, 50, 500);

CompletableFuture<Integer> result =
        pool.submit(() -> 10 * 5, 4);

result.thenAccept(v -> System.out.println("Result: " + v));

pool.shutdown();

 5. Running Tests

Ensure Maven is installed:

mvn test


Included test coverage:

✔ Future completion
✔ Backpressure rejection
✔ Priority ordering
✔ Throttling delay detection

 6. Building the Project

Compile:

mvn compile


Run the demo program:

mvn exec:java -Dexec.mainClass="com.threadpool.MainTestProgram"

 7. Performance Characteristics
Component	Complexity
Priority queue insertion	O(log n)
Worker scheduling	O(1)
Backpressure evaluation	O(1)

Optimized for high-throughput and low contention.

 8. License

This project is licensed under the MIT License.
See the LICENSE file for details.
