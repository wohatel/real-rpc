package com.murong.rpc.interaction.common;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * description
 *
 * @author yaochuang 2025/05/08 14:52
 */
public class VirtualThreadPool {
    @Getter
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public static void execute(boolean ack, Runnable task) {
        if (ack) {
            EXECUTOR.execute(task);
        }
    }

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

}
