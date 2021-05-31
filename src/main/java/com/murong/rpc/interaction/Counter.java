package com.murong.rpc.interaction;

public class Counter {
    private static long startTime;
    private static long responseCount;
    private static long requestCount;

    public static long getResponseCount() {
        return responseCount;
    }

    public static synchronized void responsePlus() {
        if (startTime == 0) {
            return;
        }
        long l = Counter.responseCount + 1;
        responseCount = l;
    }

    public static long getRequestCount() {
        return requestCount;
    }

    public static synchronized void requestPlus() {
        long l = Counter.requestCount++;
        requestCount = l;
    }

    public static synchronized void start() {
        startTime = System.currentTimeMillis();
    }

    public static double parallRate() {
        long end = System.currentTimeMillis();
        long time = end - startTime;
        return responseCount * 1000 / time;
    }

}
