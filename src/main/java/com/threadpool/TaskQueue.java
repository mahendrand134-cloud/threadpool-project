package com.threadpool;

import java.util.concurrent.PriorityBlockingQueue;

public class TaskQueue {
    private final PriorityBlockingQueue<PriorityTask> queue = new PriorityBlockingQueue<>();

    public void add(PriorityTask task) {
        queue.offer(task);
    }

    public PriorityTask take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}
