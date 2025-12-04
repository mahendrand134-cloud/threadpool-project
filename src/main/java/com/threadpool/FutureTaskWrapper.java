package com.threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps a Callable<T> into a PriorityTask and exposes a CompletableFuture<T>.
 *
 * NOTE: PriorityTask requires a (priority, Runnable) constructor; we pass
 * a no-op Runnable and override run() here to run the callable and complete the future.
 */
public class FutureTaskWrapper<T> extends PriorityTask {
    private final Callable<T> callable;
    private final CompletableFuture<T> future;

    public FutureTaskWrapper(int priority, Callable<T> callable) {
        super(priority, () -> {}); // safe placeholder for PriorityTask.task
        this.callable = callable;
        this.future = new CompletableFuture<>();
    }

    @Override
    public void run() {
        try {
            T res = callable.call();
            future.complete(res);
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }
}
