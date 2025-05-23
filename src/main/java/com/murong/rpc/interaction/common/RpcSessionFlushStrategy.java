package com.murong.rpc.interaction.common;


import lombok.Data;

@Data
public class RpcSessionFlushStrategy {

    /**
     * 该因子越小,表示刷新的越频繁,值阈为(0,1)
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
        // 已度过的session时间
        long leftTime = expiredTime - System.currentTimeMillis();
        if (leftTime < 0) {
            // 已经超时,无需刷新
            return false;
        }
        return sessionTime - leftTime > sessionTime * flushSeed;
    }


    public void reSetFlushSeed(double flushSeed) {
        if (flushSeed <= 0 || flushSeed >= 1) {
            throw new RuntimeException("flushSeed should in (0,1)");
        }
        RpcSessionFlushStrategy.flushSeed = flushSeed;
    }


    public static void main(String[] args) {
        long ex = System.currentTimeMillis() + 1500; // 2s后到期
        System.out.println(isNeedFlushForExpired(ex, 3000));
    }
}
