package com.murong.rpc.util;

import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;

@Log
public final class RunnerUtil {
    public static <T> T execSilent(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.log(Level.WARNING, "异常", e);
            return null;
        }
    }

    public static void execSilent(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.log(Level.WARNING, "异常", e);
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
            log.log(Level.WARNING, "异常", e);
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
}