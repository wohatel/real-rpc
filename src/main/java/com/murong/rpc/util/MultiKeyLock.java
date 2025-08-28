package com.murong.rpc.util;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class MultiKeyLock {

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 尝试在某个 key 上执行任务：
     *   - 如果该 key 没有人占用，则当前线程获得执行权，执行 runnable
     *   - 如果该 key 已经被占用，则直接返回，不执行
     */
    public boolean tryRunOnce(String key, Runnable task) {
        // 拿到这个 key 对应的锁
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());

        // 尝试获取锁（非阻塞）
        boolean acquired = lock.tryLock();
        if (!acquired) {
            return false; // 已经有线程在执行
        }

        try {
            task.run();
            return true;
        } finally {
            lock.unlock();
            // 释放锁后可以考虑清理无用锁，防止内存膨胀
            if (!lock.hasQueuedThreads()) {
                lockMap.remove(key, lock);
            }
        }
    }
}