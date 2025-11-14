package com.github.wohatel.util;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yaochuang 2025/05/08 14:52
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultVirtualThreadPool {

    private static class Holder {
        private static final ExecutorService EXECUTOR = createExecutor();

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

    public static ExecutorService getExecutor() {
        return Holder.EXECUTOR;
    }

    public static void execute(boolean ack, Runnable task) {
        if (ack) {
            getExecutor().execute(task);
        }
    }

    public static void execute(Runnable task) {
        execute(true, task);
    }
}
