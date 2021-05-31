package com.murong.rpc.interaction;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcAuthenticer {

    private static Map<String, Date> white;
    private static Map<String, Date> black;
    private static Map<String, Date> authing;

    /**
     * 初始化容器
     */
    public static void init() {
        if (white == null) {
            synchronized (RpcAuthenticer.class) {
                if (white == null) {
                    white = new ConcurrentHashMap<>();
                    black = new ConcurrentHashMap<>();
                    authing = new ConcurrentHashMap<>();
                }
            }
        }
    }

    /**
     * 将某ip拉黑
     *
     * @param ip
     * @param timeOut
     */
    public static void black(String ip, long timeOut) {
        black.put(ip, new Date(System.currentTimeMillis() + timeOut));
    }

    /**
     * 移除黑名单
     *
     * @param ip
     */
    public static void removeBlack(String ip) {
        black.remove(ip);
    }

    /**
     * 是否是白名单
     */
    public static boolean isBlack(String ip) {
        return black.containsKey(ip);
    }

    /**
     * 加入待认证
     *
     * @param ip
     * @param timeOut
     */
    public static void authing(String ip, long timeOut) {
        authing.put(ip, new Date(System.currentTimeMillis() + timeOut));
    }

    /**
     * 加入认证队伍
     * 默认15s
     */
    public static void authing(String ip) {
        authing.put(ip, new Date(System.currentTimeMillis() + 15_000));
    }

    /**
     * 移除待认证
     */
    public static void removeAuthing(String ip) {
        authing.remove(ip);
    }

    /**
     * 是否是待认证
     */
    public static boolean isAuthing(String ip) {
        return authing.containsKey(ip);
    }

    /**
     * 加入白名单
     */
    public static void white(String ip) {
        white.put(ip, new Date());
    }

    /**
     * 移除白名单
     */
    public static void removeWhite(String ip) {
        white.remove(ip);
    }

    /**
     * 是否是白名单
     */
    public static boolean isWhite(String ip) {
        return white.containsKey(ip);
    }


    public static boolean isOk(String ip) {
        if (isWhite(ip)) {
            return true;
        }
        if (isBlack(ip)) {
            return false;
        }
        if (isAuthing(ip)) {
            return false;
        }
        return true;
    }
}
