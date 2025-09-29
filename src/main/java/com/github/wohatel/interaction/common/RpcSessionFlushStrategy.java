package com.github.wohatel.interaction.common;


import lombok.Data;

@Data
public class RpcSessionFlushStrategy {

    /**
     * Refresh starts when the remaining time is less than the session time
     */
    public static double flushSeed = 0.5;

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
        RpcSessionFlushStrategy.flushSeed = flushSeed;
    }
}
