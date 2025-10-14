package com.github.wohatel.interaction.common;


import lombok.Data;
import lombok.Getter;

@Data
public class RpcSessionFlushStrategy {

    /**
     * Refresh starts when the remaining time is less than the session time
     */
    @Getter
    private static double flushSeed = 0.5;

    /**
     * Determine if a refresh is required
     *
     * @param expiredTime expiredTime
     * @param sessionTime sessionTime
     */
    public static boolean isNeedFlushForExpired(long expiredTime, long sessionTime) {
        return isNeedFlushForExpired(expiredTime, sessionTime, RpcSessionFlushStrategy.flushSeed);
    }

    /**
     * Determine if a refresh is required
     *
     */
    public static boolean isNeedFlushForExpired(long expiredTime, long sessionTime, double flushSeed) {
        long leftTime = expiredTime - System.currentTimeMillis();
        if (leftTime < 0) {
            return false;
        }
        if (flushSeed < 0) {
            return true;
        }
        return leftTime < sessionTime * flushSeed;
    }


    public void reSetFlushSeed(double flushSeed) {
        if (flushSeed < 0.0 || flushSeed > 1.0) {
            throw new IllegalArgumentException("flushSeed must be between 0 and 1: " + flushSeed);
        }
        RpcSessionFlushStrategy.flushSeed = flushSeed;
    }
}
