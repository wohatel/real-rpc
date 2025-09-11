package com.github.wohatel.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OneTimeLock {

    private static final Cache<String, AtomicBoolean> executed = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES) // 10分钟后过期自动清理
            .build();

    public static boolean runOnce(String key, Runnable task) {
        AtomicBoolean flag = executed.get(key, k -> new AtomicBoolean(false));
        if (flag.compareAndSet(false, true)) {
            task.run();
            return true;
        }
        return false;
    }
}