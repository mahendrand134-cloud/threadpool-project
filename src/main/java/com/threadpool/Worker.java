package com.threadpool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker thread that polls the shared task queue with a timeout so the pool can shrink.
 */
public class Worker extends Thread {
    private final TaskQueue queue;
    private final ThreadPoolExecutorCore owner;
    private final long keepAliveMillis;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public Worker(TaskQueue queue, ThreadPoolExecutorCore owner, int id, long keepAliveMillis) {
        super("ThreadPool-Worker-" + id);
        this.queue = queue;
        this.owner = owner;
        this.keepAliveMillis = keepAliveMillis;
    }

    public void shutdown() {
        running.set(false);
        this.interrupt();
    }

    @Override
    public void run() {
        try {
            while (running.get() && !isInterrupted()) {
                PriorityTask task = queue.poll(keepAliveMillis, TimeUnit.MILLISECONDS);
                if (task != null) {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        System.err.println("Task threw: " + t.getMessage());
                    }
                } else {
                    // timed out waiting -> allow owner to shrink if allowed
                    if (owner.tryShrinkWorkerIfIdle(this)) {
                        break;
                    }
                }
            }
        } catch (InterruptedException ignored) {
            // interrupted for shutdown
        } finally {
            owner.workerExited(this);
        }
    }
}
