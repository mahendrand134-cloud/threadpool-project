package com.threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps a Callable<T> into a PriorityTask and exposes a CompletableFuture<T>.
 */
public class FutureTaskWrapper<T> extends PriorityTask {
    private final Callable<T> callable;
    private final CompletableFuture<T> future;

    public FutureTaskWrapper(int priority, Callable<T> callable) {
        super(priority);
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
