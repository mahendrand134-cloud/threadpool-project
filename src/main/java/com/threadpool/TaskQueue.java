package com.threadpool;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TaskQueue {
    private final PriorityBlockingQueue<PriorityTask> queue = new PriorityBlockingQueue<>();

    public void add(PriorityTask task) {
        queue.offer(task);
    }

    public PriorityTask take() throws InterruptedException {
        return queue.take();
    }

    public PriorityTask poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int size() {
        return queue.size();
    }
}
