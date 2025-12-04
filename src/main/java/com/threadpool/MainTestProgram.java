package com.threadpool;

public class MainTestProgram {
    public static void main(String[] args) {
        ThreadPoolExecutorCore pool = new ThreadPoolExecutorCore(4, 20);

        for (int i = 0; i < 30; i++) {
            int finalI = i;

            boolean accepted = pool.submit(
                new PriorityTask(i % 5, () ->
                    System.out.println("Executing Task " + finalI +
                        " on " + Thread.currentThread().getName())
                )
            );

            if (!accepted) {
                System.out.println("Task " + i + " rejected due to backpressure!");
            }
        }

        try { Thread.sleep(2000); } catch (Exception ignored) {}

        pool.shutdown();
    }
}
