package com.murong.rpc.interaction;

public class ThreadUtil {

    public static void run(int threadCount, Runnable runnable) {
        for (int i = 0; i < threadCount; i++) {
            new Thread(runnable).start();
        }
    }
}
