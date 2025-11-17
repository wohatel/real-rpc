package com.github.wohatel.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A utility class for executing code with various error handling and retry mechanisms.
 * Provides static methods for silent execution, exception handling, and retry logic.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunnerUtil {
    /**
     * Executes a supplier function silently, catching and logging any Throwable.
     *
     * @param supplier The supplier function to execute
     * @param <T>      The return type of the supplier
     * @return The result of the supplier, or null if an exception occurs
     */
    public static <T> T execSilent(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            log.error("execSilent exception:", e);
            return null;
        }
    }

    /**
     * Executes a runnable function silently, catching and logging any Throwable.
     *
     * @param runnable The runnable function to execute
     */
    public static void execSilent(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            log.error("execSilent-runnable exception:", e);
        }
    }

    /**
     * Functional interface for a runnable that can throw an Exception.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Executes a supplier function silently, returning a default result if an exception occurs.
     *
     * @param supplier The supplier function to execute
     * @param result   The default result to return if an exception occurs
     * @param <T>      The return type of the supplier
     * @return The result of the supplier, or the default result if an exception occurs
     */
    public static <T> T execSilentExceptionTo(Supplier<T> supplier, T result) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            log.error("execSilentExceptionTo exception:", e);
            return result;
        }
    }

    /**
     * Executes a supplier function silently, applying a function to the exception to determine the result.
     * @param supplier The supplier function to execute
     * @param result The function to apply to the exception
     * @param <T> The return type of the supplier
     * @return The result of the supplier, or the result of applying the exception function
     */
    public static <T> T execSilentException(Supplier<T> supplier, Function<Exception, T> result) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("execSilentExceptionTo exception:", e);
            return result.apply(e);
        }
    }


    /**
     * Executes a supplier and handles null values and exceptions gracefully.
     *
     * @param <T>      The type of the result
     * @param supplier The supplier to execute
     * @param nullTo   The callable to return if the supplier returns null
     * @param result   The function to apply if an exception occurs
     * @return The result from the supplier, nullTo, or result function
     */
    public static <T> T execSilentNullOrException(Supplier<T> supplier, Callable<T> nullTo, Function<Exception, T> result) {
        try {
            // Execute the supplier and get the result
            T t = supplier.get();
            // Check if the result is null
            if (t == null) {
                // If null, return the result from nullTo callable
                return nullTo.call();
            } else {
                // Otherwise return the original result
                return t;
            }
        } catch (Exception e) {
            // Log the exception
            log.error("execSilentNullOrException exception:", e);
            // Return the result from the exception handler function
            return result.apply(e);
        }
    }


    /**
     * Executes a throwing runnable silently, handling any exceptions by accepting them into a consumer.
     * This method is designed to run operations that might throw exceptions without letting them propagate,
     * instead capturing and processing them through the provided consumer.
     *
     * @param throwingRunnable The runnable that might throw exceptions, wrapped in a ThrowingRunnable interface
     * @param result           A consumer that accepts any exception thrown by the runnable for further processing
     */
    public static void execSilentVoidException(ThrowingRunnable throwingRunnable, Consumer<Exception> result) {
        try {
            // Execute the provided runnable that might throw exceptions
            throwingRunnable.run();
        } catch (Exception e) {
            // Log the exception with error level for debugging purposes
            log.error("execSilentVoidException exception:", e);
            // Pass the caught exception to the consumer for further handling
            result.accept(e);
        }
    }

    /**
     * Waits for a condition to be true for a specified number of times, with a given interval.
     * Executes a runnable after the waiting period.
     * @param supplier The condition to check
     * @param times The number of times to check the condition
     * @param intervalTime The time to wait between checks
     * @param runnable The runnable to execute after waiting
     */
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

    /**
     * Tries to execute a callable multiple times until it returns a non-null value.
     * @param supplier The condition to check before each attempt
     * @param times The number of times to attempt the callable
     * @param callable The callable to execute
     * @param <T> The return type of the callable
     * @return The result of the callable, or null if all attempts return null
     */
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
     * Try n times until the test is OK
     * @param condition The condition to check
     * @param stepMillis The time to wait between checks
     * @param maxTries The maximum number of attempts
     * @return true if the condition becomes true within maxTries, false otherwise
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public static boolean waitUntil(BooleanSupplier condition, long stepMillis, long maxTries) throws InterruptedException {
        for (int i = 0; i < maxTries; i++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(stepMillis);
        }
        return condition.getAsBoolean();
    }
}