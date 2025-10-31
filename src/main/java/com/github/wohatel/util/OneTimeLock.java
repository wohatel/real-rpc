package com.github.wohatel.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OneTimeLock {

    private static Cache<@NonNull String, AtomicBoolean> executed;


    public static Cache<@NonNull String, AtomicBoolean> getExecutedInstance() {
        if (executed == null) {
            synchronized (VirtualThreadPool.class) {
                if (executed == null) {
                    executed = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
                }
            }
        }
        return executed;
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