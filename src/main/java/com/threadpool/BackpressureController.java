package com.threadpool;

public class BackpressureController {
    private final int maxQueueSize;

    public BackpressureController(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public BackpressureDecision evaluate(int currentQueueSize) {
        if (currentQueueSize > maxQueueSize)
            return BackpressureDecision.REJECT;

        if (currentQueueSize > maxQueueSize * 0.7)
            return BackpressureDecision.THROTTLE;

        return BackpressureDecision.ACCEPT;
    }
}
