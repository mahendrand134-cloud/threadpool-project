package com.threadpool;

import java.util.concurrent.CompletableFuture;

public class MainTestProgram {

    public static void main(String[] args) {

        ThreadPoolExecutorCore pool =
                new ThreadPoolExecutorCore(2, 6, 30, 1000);

        for (int i = 0; i < 20; i++) {

            final int x = i;

            CompletableFuture<Integer> result =
                    pool.submit(() -> {
                        System.out.println("Running task " + x +
                                " on " + Thread.currentThread().getName());
                        return x * 10;
                    }, i % 5);

            result.thenAccept(v ->
                    System.out.println("Completed: " + v));
        }

        try { Thread.sleep(2000); } catch (Exception ignored) {}

        pool.shutdown();
    }
}
