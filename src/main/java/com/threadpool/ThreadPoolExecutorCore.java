package com.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

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

    // throttle mechanism (no Thread.sleep)
    private final Lock throttleLock = new ReentrantLock();
    private final Condition throttleCondition = throttleLock.newCondition();

    public ThreadPoolExecutorCore(int minThreads, int maxThreads, int maxQueueSize, long keepAliveMillis) {
        if (minThreads < 1) minThreads = 1;
        if (maxThreads < minThreads) maxThreads = minThreads;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.keepAliveMillis = Math.max(100, keepAliveMillis);
        this.backpressureController = new BackpressureController(maxQueueSize);

        for (int i = 0; i < this.minThreads; i++) {
            spawnWorker();
        }
    }

    public ThreadPoolExecutorCore(int threads, int maxQueueSize) {
        this(threads, threads, maxQueueSize, 1000);
    }

    private synchronized void spawnWorker() {
        if (!running) return;
        if (currentThreads.get() >= maxThreads) return;

        int id = nextWorkerId.getAndIncrement();
        Worker w = new Worker(taskQueue, this, id, keepAliveMillis);
        workers.add(w);
        currentThreads.incrementAndGet();
        w.start();
    }

    private void throttleWait() {
        throttleLock.lock();
        try {
            throttleCondition.awaitNanos(50_000_000L); // 50ms but NOT Thread.sleep
        } catch (InterruptedException ignored) {}
        finally {
            throttleLock.unlock();
        }
    }

    public <T> CompletableFuture<T> submit(Callable<T> callable, int priority) {
        if (!running) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(new RejectedExecutionException("Pool shutting down"));
            return f;
        }

        int queueSize = taskQueue.size();
        int activeThreads = currentThreads.get();

        BackpressureDecision decision =
                backpressureController.evaluate(queueSize, activeThreads, maxThreads);

        if (decision == BackpressureDecision.REJECT) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(new RejectedExecutionException("Rejected by backpressure"));
            return f;
        }

        FutureTaskWrapper<T> wrapper = new FutureTaskWrapper<>(priority, callable);
        taskQueue.add(wrapper);

        if (decision == BackpressureDecision.THROTTLE) {
            throttleWait();
        }

        maybeGrowWorkers();
        return wrapper.getFuture();
    }

    private void maybeGrowWorkers() {
        int qsize = taskQueue.size();
        int threads = currentThreads.get();

        if (threads < maxThreads && qsize > threads) {
            spawnWorker();
        }
    }

    synchronized boolean tryShrinkWorkerIfIdle(Worker worker) {
        if (!running) return true;
        if (currentThreads.get() > minThreads) {
            workers.remove(worker);
            currentThreads.decrementAndGet();
            return true;
        }
        return false;
    }

    synchronized void workerExited(Worker w) {
        workers.remove(w);
        if (currentThreads.get() > 0)
            currentThreads.decrementAndGet();
    }

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
}
