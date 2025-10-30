package com.github.wohatel.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OneTimeLock {

    private static final Cache<String, AtomicBoolean> executed = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES)// Automatically cleaned up after 3 minutes
            .build();

    /**
     * A key is executed only once
     */
    public static boolean runOnce(String key, Runnable task) {
        AtomicBoolean flag = executed.get(key, k -> new AtomicBoolean(false));
        if (flag.compareAndSet(false, true)) {
            task.run();
            return true;
        }
        return false;
    }
}