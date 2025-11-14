package com.github.wohatel.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OneTimeLock {

    private static class ExecutedHolder {
        static final Cache<String, AtomicBoolean> INSTANCE =
                Caffeine.newBuilder()
                        .expireAfterWrite(20, TimeUnit.MINUTES)
                        .build();
    }

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