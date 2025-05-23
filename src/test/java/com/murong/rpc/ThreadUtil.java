package com.murong.rpc;

import lombok.SneakyThrows;

import java.util.function.BooleanSupplier;

public class ThreadUtil {

    public static void run(int threadCount, Runnable runnable) {
        for (int i = 0; i < threadCount; i++) {
            new Thread(runnable).start();
        }
    }

    @SneakyThrows
    public static void waitTimesFor(BooleanSupplier supplier, int times, long intervalTime, Runnable runnable) {
        for (int i = 0; i < times; i++) {
            boolean asBoolean = supplier.getAsBoolean();
            if (asBoolean) {
                break;
            }
            Thread.sleep(intervalTime);
        }
        runnable.run();
    }
}
