package com.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadPoolExecutorCore with:
 * - minThreads / maxThreads (cached strategy)
 * - keepAliveMillis for idle worker shrink
 * - submit(Callable<T>, priority) returning CompletableFuture<T>
 *
 * Backwards compatible constructor ThreadPoolExecutorCore(int threads, int maxQueueSize)
 * is kept for fixed-size usage.
 */
public class ThreadPoolExecutorCore {
    private final TaskQueue taskQueue = new TaskQueue();
    private final BackpressureController backpressureController;
    private final List<Worker> workers = new ArrayList<>();
    private final AtomicInteger nextWorkerId = new AtomicInteger(0);
    private final AtomicInteger currentThreads = new AtomicInteger(0);

    private final int minThreads;
    private final int maxThreads;
    private final long keepAliveMillis;

    private volatile boolean running = true;

    /**
     * Full constructor supporting cached behaviour.
     *
     * @param minThreads minimum threads to keep alive
     * @param maxThreads maximum allowed threads
     * @param maxQueueSize maximum queue size before rejecting
     * @param keepAliveMillis idle timeout for extra workers (ms)
     */
    public ThreadPoolExecutorCore(int minThreads, int maxThreads, int maxQueueSize, long keepAliveMillis) {
        if (minThreads < 1) minThreads = 1;
        if (maxThreads < minThreads) maxThreads = minThreads;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.keepAliveMillis = Math.max(100, keepAliveMillis);
        this.backpressureController = new BackpressureController(maxQueueSize);

        // start minThreads
        for (int i = 0; i < this.minThreads; i++) {
            spawnWorker();
        }
    }

    /**
     * Legacy convenience constructor for fixed-size pool.
     */
    public ThreadPoolExecutorCore(int threads, int maxQueueSize) {
        this(threads, threads, maxQueueSize, 1000);
    }

    // spawn a new worker (synchronized to protect workers list)
    private synchronized void spawnWorker() {
        if (!running) return;
        if (currentThreads.get() >= maxThreads) return;
        int id = nextWorkerId.getAndIncrement();
        Worker w = new Worker(taskQueue, this, id, keepAliveMillis);
        workers.add(w);
        currentThreads.incrementAndGet();
        w.start();
    }

    /**
     * Legacy submission accepting PriorityTask.
     */
    public boolean submit(PriorityTask task) {
        BackpressureDecision decision = backpressureController.evaluate(taskQueue.size());

        if (decision == BackpressureDecision.REJECT) {
            return false;
        }

        taskQueue.add(task);

        if (decision == BackpressureDecision.THROTTLE) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        maybeGrowWorkers();
        return true;
    }

    /**
     * Modern submit that accepts a Callable and returns a CompletableFuture.
     * If rejected due to backpressure or pool shutdown, returns exceptionally completed future.
     */
    public <T> CompletableFuture<T> submit(Callable<T> callable, int priority) {
        if (!running) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(new RejectedExecutionException("Pool not running"));
            return f;
        }

        BackpressureDecision decision = backpressureController.evaluate(taskQueue.size());
        if (decision == BackpressureDecision.REJECT) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(new RejectedExecutionException("Rejected by backpressure"));
            return f;
        }

        FutureTaskWrapper<T> wrapper = new FutureTaskWrapper<>(priority, callable);
        taskQueue.add(wrapper);

        if (decision == BackpressureDecision.THROTTLE) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        maybeGrowWorkers();
        return wrapper.getFuture();
    }

    // If queue backlog is growing try to add worker up to maxThreads
    private void maybeGrowWorkers() {
        int qsize = taskQueue.size();
        int threads = currentThreads.get();
        // simple heuristic: if queue size > threads and we can add more workers
        if (qsize > threads && threads < maxThreads) {
            spawnWorker();
        }
    }

    /**
     * Called by Worker when it timed out waiting and wants to exit if pool has more than minThreads.
     * Returns true if the worker should exit (owner removed it from the list).
     */
    synchronized boolean tryShrinkWorkerIfIdle(Worker worker) {
        if (!running) return true; // during shutdown allow exit
        if (currentThreads.get() > minThreads) {
            boolean removed = workers.remove(worker);
            if (removed) {
                currentThreads.decrementAndGet();
            }
            return true;
        }
        return false;
    }

    // called by worker when it exits to keep counts consistent
    synchronized void workerExited(Worker w) {
        workers.remove(w);
        int cnt = currentThreads.get();
        if (cnt > 0) currentThreads.decrementAndGet();
    }

    /**
     * Graceful shutdown: signal all workers to stop and clear worker list.
     */
    public void shutdown() {
        running = false;
        synchronized (this) {
            for (Worker w : new ArrayList<>(workers)) {
                w.shutdown();
            }
            workers.clear();
            currentThreads.set(0);
        }
    }

    public int getCurrentThreadCount() {
        return currentThreads.get();
    }

    public int getQueueSize() {
        return taskQueue.size();
    }
}
