package com.github.wohatel.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * description
 *
 * @author yaochuang 2026/01/15 10:11
 */
public class GoalKeeper {

    private final ConcurrentHashMap<String, AtomicBoolean> onceMap = new ConcurrentHashMap<>();

    public boolean once(String key) {
        return onceMap.computeIfAbsent(key, k -> new AtomicBoolean(false)).compareAndSet(false, true);
    }

}
