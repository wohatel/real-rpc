package com.github.wohatel.interaction.common;


import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class defines the flush strategy for RPC sessions.
 * It provides methods to determine when a session needs to be refreshed
 * and allows configuration of the flush seed value.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSessionFlushStrategy {

    /**
     * Refresh starts when the remaining time is less than the session time
     * This is the golden ratio value used as the default threshold for session refresh
     */
    @Getter
    private static double flushSeed = 0.618;

    /**
     * Determine if a refresh is required based on expired time and session time
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


    /**
     * Sets the flush seed value for the RPC session flush strategy.
     * The flush seed determines the probability at which the session will be flushed.
     *
     * @param flushSeed the new flush seed value, which must be between 0.0 and 1.0 (inclusive)
     * @throws IllegalArgumentException if the flush seed is not within the valid range [0.0, 1.0]
     */
    public static void reSetFlushSeed(double flushSeed) {
        // Validate that the flush seed is within the allowed range [0.0, 1.0]
        if (flushSeed < 0.0 || flushSeed > 1.0) {
            throw new IllegalArgumentException("flushSeed must be between 0 and 1: " + flushSeed);
        }
        // Update the static flush seed value in the RpcSessionFlushStrategy
        RpcSessionFlushStrategy.flushSeed = flushSeed;
    }
}
