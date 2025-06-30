package com.murong.rpc.interaction.common;

import com.murong.rpc.interaction.constant.NumberConstant;
import lombok.extern.java.Log;

/**
 * description
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Log
public class RpcSessionManager {

    /**
     * 文件块的释放,需要比较久的时间,为了避免单线程造成的资源关闭堆积,才出采用线程池= true
     */
    private static final SessionManager<RpcSessionContext> SESSION_MANAGER = new SessionManager<>(NumberConstant.THREE_TEN_K);

    private static final RpcSessionContext EMPTY_CONTEXT = new RpcSessionContext();

    /**
     * 是否正常运行
     */
    public static boolean isRunning(String sessionId) {
        return SESSION_MANAGER.contains(sessionId);
    }

    /**
     * 是否正常运行
     */
    public static RpcSessionContext getContext(String sessionId) {
        RpcSessionContext session = SESSION_MANAGER.getSession(sessionId);
        if (session == EMPTY_CONTEXT) {
            return null;
        }
        return session;
    }

    /**
     * 初始化
     *
     * @param sessionId
     * @param context
     * @param expiredAt
     */
    public static void init(String sessionId, RpcSessionContext context, Long expiredAt) {
        if (context == null) {
            SESSION_MANAGER.initSession(sessionId, EMPTY_CONTEXT, expiredAt);
        } else {
            SESSION_MANAGER.initSession(sessionId, context, expiredAt);
        }
    }

    /**
     * 刷新时间
     *
     * @param sessionId
     * @param sessionTime
     */
    public static boolean flush(String sessionId, long sessionTime) {
        return SESSION_MANAGER.flushTime(sessionId, sessionTime);
    }

    /**
     * 释放session
     */
    public static void release(String id) {
        SESSION_MANAGER.release(id);
    }

}
