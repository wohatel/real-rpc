package com.github.wohatel.util;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yaochuang 2025/05/08 14:52
 */
public class VirtualThreadPool {

    private static volatile ExecutorService executor;

    public static ExecutorService getExecutorInstance() {
        if (executor == null) {
            synchronized (VirtualThreadPool.class) {
                if (executor == null) {
                    executor = Executors.newVirtualThreadPerTaskExecutor();
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (!executor.isShutdown()) {
                            executor.shutdown();
                        }
                    }));
                }
            }
        }
        return executor;
    }

    public static void execute(boolean ack, Runnable task) {
        if (ack) {
            getExecutorInstance().execute(task);
        }
    }

    public static void execute(Runnable task) {
        execute(true, task);
    }
}
