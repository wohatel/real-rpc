package com.murong.rpc.util;

import com.murong.rpc.interaction.constant.NumberConstant;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * 高性能、低对象创建的限速器，用于传输速率控制（单位：B/s）
 */
public class RpcSpeedLimiter {

    // 速率限制（B/s）
    private final AtomicLong speedLimit = new AtomicLong(NumberConstant.M_10);

    // 窗口粒度（500ms）和保留时间（5秒 => 50 个桶）
    private static final int BUCKET_DURATION_MS = 500;
    private static final int MAX_BUCKETS = 20;

    // 每个桶记录的字节数和时间戳（以 100ms 为单位）
    private final long[] timeBuckets = new long[MAX_BUCKETS];
    private final long[] timeStamps = new long[MAX_BUCKETS]; // 单位：now / 100

    public RpcSpeedLimiter(long speed) {
        if (speed > 0) {
            speedLimit.set(speed);
        } else {
            speedLimit.set(NumberConstant.M_10);
        }
    }

    /**
     * 刷新发送字节数
     */
    public synchronized void flush(long bytes) {
        long now = System.currentTimeMillis();
        long nowTick = now / BUCKET_DURATION_MS;
        int index = (int) (nowTick % MAX_BUCKETS);

        if (timeStamps[index] != nowTick) {
            timeBuckets[index] = 0;
            timeStamps[index] = nowTick;
        }

        timeBuckets[index] += bytes;
    }

    /**
     * 获取过去 5 秒的平均速度（单位：KB/s）
     */
    public synchronized BigDecimal currentSpeed() {
        long nowTick = System.currentTimeMillis() / BUCKET_DURATION_MS;
        long totalBytes = 0;
        long earliestTick = Long.MAX_VALUE;

        for (int i = 0; i < MAX_BUCKETS; i++) {
            long tick = timeStamps[i];
            if (nowTick - tick < MAX_BUCKETS) {
                totalBytes += timeBuckets[i];
                if (tick < earliestTick) {
                    earliestTick = tick;
                }
            }
        }

        if (totalBytes == 0 || earliestTick == Long.MAX_VALUE) {
            return BigDecimal.ZERO;
        }

        long elapsedMs = (nowTick - earliestTick) * BUCKET_DURATION_MS;
        if (elapsedMs <= 0) {
            elapsedMs = BUCKET_DURATION_MS;
        }
        return BigDecimal.valueOf(totalBytes).divide(BigDecimal.valueOf(elapsedMs / 1000.0), 3, RoundingMode.HALF_UP);
    }

    /**
     * 是否超过速率限制
     */
    public boolean isBeyondSpeed() {
        return currentSpeed().doubleValue() > speedLimit.get();
    }

    /**
     * 阻塞等待直到速度降到限速范围内
     */
    @SneakyThrows
    public boolean waitSpeed(long stepMillis, long maxTries) {
        return waitUntil(() -> !this.isBeyondSpeed(), stepMillis, maxTries);
    }

    /**
     * 通用等待函数
     */
    @SneakyThrows
    public static boolean waitUntil(BooleanSupplier condition, long stepMillis, long maxTries) {
        for (int i = 0; i < maxTries; i++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(stepMillis);
        }
        return condition.getAsBoolean();
    }

    @SneakyThrows
    public static boolean waitUntil(BooleanSupplier condition, long stepMillis, long maxTries, Runnable runnable) {
        for (int i = 0; i < maxTries; i++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            if (runnable != null) {
                runnable.run();
            }
            Thread.sleep(stepMillis);
        }
        return condition.getAsBoolean();
    }
}