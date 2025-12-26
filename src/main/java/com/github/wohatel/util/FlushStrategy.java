package com.github.wohatel.util;

import java.util.function.BiPredicate;

/**
 * description
 *
 * @author yaochuang 2025/12/25 17:05
 */
public class FlushStrategy {


    public static BiPredicate<Long, Long> buildDefault(Long sessionTime, double flushSeed) {
        return (oldExpiredTime, newExpiredTime) -> {
            // 已过期
            if (oldExpiredTime > System.currentTimeMillis()) {
                return false;
            }
            // 如果新的过期时间要比旧的过期时间更靠前,则以新的为准
            if (newExpiredTime < oldExpiredTime) {
                return true;
            }
            long leftTime = oldExpiredTime - System.currentTimeMillis();
            boolean timeEnough = leftTime > sessionTime * flushSeed;
            // 时间充裕,不用刷新
            if (timeEnough) {
                return false;
            } else {
                return true;
            }
        };
    }

    public static BiPredicate<Long, Long> buildDefault(Long sessionTime) {
        return buildDefault(sessionTime, 0.618);
    }

}
