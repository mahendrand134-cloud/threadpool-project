package com.threadpool;

import java.util.ArrayList;
import java.util.List;

public class ThreadPoolExecutorCore {
    private final TaskQueue taskQueue = new TaskQueue();
    private final BackpressureController backpressureController;
    private final List<Worker> workers = new ArrayList<>();

    public ThreadPoolExecutorCore(int threads, int maxQueueSize) {
        backpressureController = new BackpressureController(maxQueueSize);

        for (int i = 0; i < threads; i++) {
            Worker w = new Worker(taskQueue);
            workers.add(w);
            w.start();
        }
    }

    public boolean submit(PriorityTask task) {
        BackpressureDecision decision =
            backpressureController.evaluate(taskQueue.size());

        if (decision == BackpressureDecision.REJECT) {
            return false;
        }

        taskQueue.add(task);

        if (decision == BackpressureDecision.THROTTLE) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        return true;
    }

    public void shutdown() {
        for (Worker w : workers) {
            w.shutdown();
        }
    }
}
