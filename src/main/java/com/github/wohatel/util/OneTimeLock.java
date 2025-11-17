package com.github.wohatel.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A utility class that provides a mechanism to execute a task only once for a specific key.
 * Uses Caffeine cache with 20 minutes expiration time to track executed tasks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OneTimeLock {

    /**
     * A static nested holder class for the cache instance using initialization-on-demand idiom.
     * This ensures thread-safe lazy initialization of the cache.
     */
    private static class ExecutedHolder {
        /**
         * A static final cache that stores execution flags as AtomicBoolean values.
         * The cache expires entries 20 minutes after they are written.
         */
        static final Cache<String, AtomicBoolean> INSTANCE =
                Caffeine.newBuilder()
                        .expireAfterWrite(20, TimeUnit.MINUTES)
                        .build();
    }

    /**
     * Returns the cache instance that tracks executed tasks.
     *
     * @return The cache instance containing execution flags
     */
    public static Cache<String, AtomicBoolean> getExecutedInstance() {
        return ExecutedHolder.INSTANCE;
    }

    /**
     * A key is executed only once
     */
    public static boolean runOnce(String key, Runnable task) {
        AtomicBoolean flag = getExecutedInstance().get(key, k -> new AtomicBoolean(false));
        if (flag.compareAndSet(false, true)) {
            task.run();
            return true;
        }
        return false;
    }
}