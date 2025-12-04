package com.threadpool;

public class PriorityTask implements Comparable<PriorityTask>, Runnable {
    private final int priority;
    private final Runnable task;

    public PriorityTask(int priority, Runnable task) {
        this.priority = priority;
        this.task = task;
    }

    @Override
    public int compareTo(PriorityTask other) {
        // Higher priority first
        return Integer.compare(other.priority, this.priority);
    }

    @Override
    public void run() {
        task.run();
    }
}
