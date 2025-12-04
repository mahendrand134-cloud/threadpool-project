package com.threadpool;

/**
 * Simple backpressure logic based on queue usage.
 * - ACCEPT: low usage
 * - THROTTLE: moderate (delay submit slightly)
 * - REJECT: beyond max queue size
 */
public class BackpressureController {
    private final int maxQueueSize;

    public BackpressureController(int maxQueueSize) {
        this.maxQueueSize = Math.max(1, maxQueueSize);
    }

    public BackpressureDecision evaluate(int currentQueueSize) {
        if (currentQueueSize > maxQueueSize) {
            return BackpressureDecision.REJECT;
        }
        if (currentQueueSize > (int)(maxQueueSize * 0.75)) {
            return BackpressureDecision.THROTTLE;
        }
        return BackpressureDecision.ACCEPT;
    }
}
