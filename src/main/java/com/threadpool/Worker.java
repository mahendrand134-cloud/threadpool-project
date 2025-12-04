package com.threadpool;

public class Worker extends Thread {
    private final TaskQueue queue;
    private volatile boolean running = true;

    public Worker(TaskQueue queue) {
        this.queue = queue;
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                PriorityTask task = queue.take();
                task.run();
            } catch (InterruptedException ignored) {}
        }
    }
}
