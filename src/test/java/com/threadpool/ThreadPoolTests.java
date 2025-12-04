package com.threadpool;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadPoolTests {

    @Test
    public void testSubmitAndFuturesComplete() throws Exception {
        // cached pool: min 2, max 4, queue size large enough for the test
        ThreadPoolExecutorCore pool = new ThreadPoolExecutorCore(2, 4, 50, 500);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        int n = 20;
        for (int i = 0; i < n; i++) {
            final int v = i;
            CompletableFuture<Integer> f = pool.submit(() -> {
                // short work
                Thread.sleep(20);
                return v * 2;
            }, i % 5);
            futures.add(f);
        }

        // Wait for completion (timeout to avoid hanging tests)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        for (int i = 0; i < n; i++) {
            assertEquals(i * 2, futures.get(i).getNow(null),
                "Future result mismatch at index " + i);
        }

        pool.shutdown();
    }

    @Test
    public void testBackpressureRejectsWhenQueueFull() throws Exception {
        // tiny pool and small queue so backpressure triggers quickly
        // minThreads = 1, maxThreads = 1 => single worker
        // maxQueueSize = 4 so after queue > 4, REJECT kicks in
        ThreadPoolExecutorCore pool = new ThreadPoolExecutorCore(1, 1, 4, 500);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        int submissions = 40;
        for (int i = 0; i < submissions; i++) {
            final int v = i;
            CompletableFuture<Integer> f = pool.submit(() -> {
                // long task to fill queue and keep workers busy
                Thread.sleep(300);
                return v;
            }, 0);
            futures.add(f);
        }

        // Count how many were rejected quickly (RejectedExecutionException)
        int rejected = 0;
        for (CompletableFuture<Integer> f : futures) {
            try {
                f.get(100, TimeUnit.MILLISECONDS);
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof RejectedExecutionException) {
                    rejected++;
                }
            } catch (Exception ignored) {
            }
        }

        assertTrue(rejected > 0, "Expected some rejections due to backpressure");
        pool.shutdown();
    }

    @Test
    public void testPriorityOrderingWithSingleWorker() throws Exception {
        // Use single worker so execution order strictly follows queue ordering
        ThreadPoolExecutorCore pool = new ThreadPoolExecutorCore(1, 1, 50, 500);

        // We'll submit five tasks with varying priorities and record the execution order
        List<Integer> executed = Collections.synchronizedList(new ArrayList<>());

        // (id, priority)
        List<int[]> tasks = Arrays.asList(
            new int[]{0, 1},
            new int[]{1, 4},
            new int[]{2, 2},
            new int[]{3, 3},
            new int[]{4, 0}
        );

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int[] t : tasks) {
            final int id = t[0];
            final int pr = t[1];
            CompletableFuture<Void> f = pool.submit(() -> {
                executed.add(id);
                return null;
            }, pr);
            futures.add(f);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        // Expected order: sorted by priority desc, then FIFO among same priority.
        List<Integer> expected = tasks.stream()
            .sorted((a, b) -> Integer.compare(b[1], a[1])) // priority desc
            .map(a -> a[0])
            .collect(Collectors.toList());

        assertEquals(expected, executed, "Execution order should follow priority (desc) then FIFO");
        pool.shutdown();
    }

    @Test
    public void testBackpressureThrottlingCausesSubmitDelay() throws Exception {
        // Configure a small queue and a throttle threshold so we can observe a small delayed submission
        // maxQueueSize = 4 -> throttle when queue > 0.75*4 => queue > 3
        ThreadPoolExecutorCore pool = new ThreadPoolExecutorCore(1, 1, 4, 500);

        // Submit long-running tasks until queue reaches throttling zone
        List<CompletableFuture<Integer>> longTasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int v = i;
            longTasks.add(pool.submit(() -> {
                Thread.sleep(400); // keep worker busy
                return v;
            }, 0));
        }

        // Now perform several rapid submissions and measure time of each submit call
        List<Long> durations = new ArrayList<>();
        int extraSubmissions = 6;
        for (int i = 0; i < extraSubmissions; i++) {
            long start = System.nanoTime();
            CompletableFuture<Integer> f = pool.submit(() -> {
                // short task
                return 1;
            }, 0);
            long end = System.nanoTime();
            durations.add(TimeUnit.NANOSECONDS.toMillis(end - start));
            // store future to avoid GC etc.
            final CompletableFuture<Integer> ff = f;
            ff.exceptionally(ex -> null);
        }

        // At least one submission should have been throttled (we used 50ms sleep for throttle)
        boolean sawDelay = durations.stream().anyMatch(d -> d >= 40); // 40ms threshold is tolerant
        assertTrue(sawDelay, "Expected at least one submission to be throttled/delayed");

        // clean up
        CompletableFuture.allOf(longTasks.toArray(new CompletableFuture[0])).join();
        pool.shutdown();
    }
}
