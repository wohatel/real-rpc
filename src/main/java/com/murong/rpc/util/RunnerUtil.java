package com.murong.rpc.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
public final class RunnerUtil {
    public static <T> T execSilent(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("execSilent异常", e);
            return null;
        }
    }

    public static void execSilent(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("execSilent-runnable异常", e);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static <T> T execSilentExceptionTo(Supplier<T> supplier, T result) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("execSilentExceptionTo异常", e);
            return result;
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

    @SneakyThrows
    public static <T> T tryTimesUntilNotNull(BooleanSupplier supplier, int times, Callable<T> callable) {
        for (int i = 0; i < times; i++) {
            if (supplier.getAsBoolean()) {
                T call = callable.call();
                if (call != null) {
                    return call;
                }
            }
        }
        return null;
    }

    /**
     * 尝试n次,知道测试ok
     */
    public static boolean waitUntil(BooleanSupplier condition, long stepMillis, long maxTries) {
        for (int i = 0; i < maxTries; i++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            RunnerUtil.sleep(stepMillis);
        }
        return condition.getAsBoolean();
    }

    /**
     * 包装线程sleep
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

}