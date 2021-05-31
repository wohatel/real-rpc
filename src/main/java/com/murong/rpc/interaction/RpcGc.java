package com.murong.rpc.interaction;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RpcGc {

    private static ScheduledExecutorService executorService;
    private static int handleLimit = 20_0000; // 20_0000,就开始清理启动清理数据
    private static int handleLimtUpTime = 1000_000; // 1000s未清理,则必须清理
    private static long lastHandleOutTime = System.currentTimeMillis(); // 上次gc时间

    public static void callWake() {
        if (executorService == null) {
            synchronized (RpcInteractionContainer.class) {
                if (executorService == null) {
                    executorService = Executors.newSingleThreadScheduledExecutor();
                    executorService.scheduleWithFixedDelay(RpcGc::handleOutTime, 0l, 4000, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public static void handleOutTime() {
        if (needClean()) {
            Set<Map.Entry<String, RpcFuture>> entries = RpcInteractionContainer.getFutureMap().entrySet();
            Iterator<Map.Entry<String, RpcFuture>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, RpcFuture> next = iterator.next();
                RpcFuture value = next.getValue();
                long timeOut = value.getTimeOut();
                long requestTime = value.getRequestTime();
                long time = System.currentTimeMillis() - requestTime;
                if (time >= timeOut) {
                    iterator.remove(); // 如果超时,就直接删除
                }
            }
            lastHandleOutTime = System.currentTimeMillis();
        }
    }

    private static boolean needClean() {
        //1 如果已累计积压超过 handleLimit 万的并发必须清理
        if (RpcInteractionContainer.concurrentSize() >= handleLimit) {
            return true;
        }
        //2 如果累计handleLimtUpTime 事件内未清理,则必须清理
        long nowTime = System.currentTimeMillis();
        long millSeconds = nowTime - lastHandleOutTime; // 与上次的清理时间间隔
        if (millSeconds >= handleLimtUpTime) {
            return true;
        }
        return false;
    }

    /**
     * 当容器中数量超过 handleLimit 时,必须及时开始gc
     *
     * @param handleLimit
     */
    public static void configHandleLimit(int handleLimit) {
        RpcGc.handleLimit = handleLimit;
    }

    /**
     * 当上次gc时间间隔时间超过 handleLimtUpTime ms时,时必须及时开始gc
     *
     * @param handleLimtUpTime
     */
    public static void configHandleLimtUpTime(int handleLimtUpTime) {
        RpcGc.handleLimtUpTime = handleLimtUpTime;
    }

}

