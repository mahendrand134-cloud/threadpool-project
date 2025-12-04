package com.threadpool;

/**
 * Improved BackpressureController
 *
 * Now evaluates:
 *  - queue size percentage
 *  - queue growth rate (fast growth → throttle)
 *  - active thread utilization (proxy for CPU load)
 */
public class BackpressureController {

    private final int maxQueueSize;

    // For growth-rate estimation
    private int lastQueueSize = 0;
    private long lastCheckTime = System.nanoTime();

    public BackpressureController(int maxQueueSize) {
        this.maxQueueSize = Math.max(1, maxQueueSize);
    }

    public BackpressureDecision evaluate(int queueSize, int activeThreads, int maxThreads) {

        // --- 1. Queue overflow (hard reject)
        if (queueSize > maxQueueSize) {
            return BackpressureDecision.REJECT;
        }

        // --- 2. Queue growth rate (fast growth → throttle)
        long now = System.nanoTime();
        long elapsedMs = (now - lastCheckTime) / 1_000_000;

        BackpressureDecision decision = BackpressureDecision.ACCEPT;

        if (elapsedMs > 50) {  // evaluate growth every 50ms
            int growth = queueSize - lastQueueSize;

            if (growth > maxQueueSize * 0.20) { // grew 20% in one check
                decision = BackpressureDecision.THROTTLE;
            }

            lastQueueSize = queueSize;
            lastCheckTime = now;
        }

        // --- 3. Thread utilization (proxy for CPU load)
        if (activeThreads >= maxThreads && queueSize > maxQueueSize * 0.50) {
            decision = BackpressureDecision.THROTTLE;
        }

        // --- 4. Queue pressure
        if (queueSize > maxQueueSize * 0.75) {
            decision = BackpressureDecision.THROTTLE;
        }

        return decision;
    }
}
