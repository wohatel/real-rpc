package com.murong.rpc.interaction.common;


import lombok.Data;

@Data
public class RpcSessionFlushStrategy {

    /**
     * 剩余时间小于session时间的多少时开始刷新
     */
    public static double flushSeed = 0.5;

    /**
     * 判断是否需要刷新
     *
     * @param expiredTime 过期时间
     * @param sessionTime session规定的时间
     * @return 结果
     */
    public static boolean isNeedFlushForExpired(long expiredTime, long sessionTime) {
        return isNeedFlushForExpired(expiredTime, sessionTime, RpcSessionFlushStrategy.flushSeed);
    }

    /**
     * 判断是否需要刷新
     *
     * @param expiredTime 过期时间
     * @param sessionTime session规定的时间
     * @return 结果
     */
    public static boolean isNeedFlushForExpired(long expiredTime, long sessionTime, double flushSeed) {
        // 已度过的session时间
        long leftTime = expiredTime - System.currentTimeMillis();
        if (leftTime < 0) {
            // 已经超时,无需刷新
            return false;
        }
        // 如果刷新因子为负数,直接刷新
        if (flushSeed < 0) {
            return true;
        }
        return leftTime < sessionTime * flushSeed;
    }


    public void reSetFlushSeed(double flushSeed) {
        RpcSessionFlushStrategy.flushSeed = flushSeed;
    }
}
