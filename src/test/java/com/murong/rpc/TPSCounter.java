package com.murong.rpc;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单的TPS（Transactions Per Second）统计器。
 */
public class TPSCounter {
    private final AtomicLong counter = new AtomicLong(0);
    private final AtomicLong lastTimestamp = new AtomicLong(System.currentTimeMillis());


    public TPSCounter() {
    }

    /**
     * 调用一次，表示完成了一次"事务"
     */
    public void increment() {
        counter.incrementAndGet();
    }

    /**
     * 检查是否到达统计间隔，如果到达则返回当前TPS，并重置计数器。
     * 如果没到达，返回-1。
     */
    public long report() {
        long now = System.currentTimeMillis();
        long last = lastTimestamp.get();
        return counter.get() * 1000 / (now - last + 1);
    }
}