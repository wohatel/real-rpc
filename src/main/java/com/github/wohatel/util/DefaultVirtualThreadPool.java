package com.github.wohatel.util;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DefaultVirtualThreadPool is a utility class that provides a virtual thread executor service.
 * This implementation uses the singleton pattern to ensure a single instance of the executor service.
 * The executor is configured to create a new virtual thread for each task.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultVirtualThreadPool {

    /**
     * Holder class is used for lazy initialization of the executor service.
     * This follows the initialization-on-demand idiom thread-safe singleton pattern.
     */
    private static class Holder {
        /**
         * Static final instance of ExecutorService that creates a new virtual thread per task.
         * This executor will be initialized when it's first accessed.
         */
        private static final ExecutorService EXECUTOR = createExecutor();

        /**
         * Creates and configures the ExecutorService instance.
         * Sets up a shutdown hook to ensure proper cleanup when the JVM shuts down.
         *
         * @return a configured ExecutorService instance
         */
        private static ExecutorService createExecutor() {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!executor.isShutdown()) {
                    executor.shutdown();
                }
            }));
            return executor;
        }
    }

    /**
     * Returns the singleton instance of the ExecutorService.
     * The executor is created on first access.
     *
     * @return the ExecutorService instance
     */
    public static ExecutorService getExecutor() {
        return Holder.EXECUTOR;
    }

    /**
     * Executes a task using the virtual thread executor.
     * The task will be executed only if the ack parameter is true.
     *
     * @param ack  flag indicating whether the task should be executed
     * @param task the Runnable task to be executed
     */
    public static void execute(boolean ack, Runnable task) {
        if (ack) {
            getExecutor().execute(task);
        }
    }

    /**
     * Executes a task using the virtual thread executor.
     * This is a convenience method that always executes the task (ack = true).
     *
     * @param task the Runnable task to be executed
     */
    public static void execute(Runnable task) {
        execute(true, task);
    }
}
