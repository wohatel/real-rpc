package com.murong.rpc.interaction.common;


import com.murong.rpc.interaction.handler.RpcResponseMsgListener;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;

import java.util.ArrayList;
import java.util.List;

public class RpcInteractionContainer {

    private static final SessionManager<RpcFuture> RPC_FUTURE_SESSION_MANAGER = new SessionManager<>(NumberConstant.OVER_TIME, RpcInteractionContainer::handleTimeOut);

    public static RpcSessionFuture sendSessionRequest(RpcSessionRequest rpcSessionRequest) {
        if (rpcSessionRequest == null) {
            return null;
        }
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        if (rpcSession == null) {
            throw new RuntimeException("session 标识不能为null");
        }
        if (contains(rpcSession.getSessionId())) {
            RpcSessionFuture rpcFuture = getSessionFuture(rpcSession.getSessionId());
            if (!rpcFuture.isSessionFinish()) {
                rpcFuture.setRequestTime(System.currentTimeMillis());
                Long expireAt = RPC_FUTURE_SESSION_MANAGER.getExpireAt(rpcSession.getSessionId());
                if (RpcSessionFlushStrategy.isNeedFlushForExpired(expireAt, rpcSession.getTimeOutMillis())) {
                    // 刷新时间
                    RPC_FUTURE_SESSION_MANAGER.flushTime(rpcSession.getSessionId(), System.currentTimeMillis() + rpcSession.getTimeOutMillis());
                }
            }
            return rpcFuture;
        }
        RpcSessionFuture rpcFuture = new RpcSessionFuture(rpcSession.getTimeOutMillis());
        rpcFuture.setRequestId(rpcSession.getSessionId());
        RPC_FUTURE_SESSION_MANAGER.initSession(rpcSession.getSessionId(), rpcFuture, System.currentTimeMillis() + rpcSession.getTimeOutMillis());
        return rpcFuture;
    }

    public static RpcSessionFuture getSessionFuture(String sessionId) {
        return (RpcSessionFuture) RPC_FUTURE_SESSION_MANAGER.getSession(sessionId);
    }

    public static void addResponse(RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            return;
        }
        if (rpcResponse.getRequestId() == null) {
            return;
        }
        RpcFuture rpcFuture = RPC_FUTURE_SESSION_MANAGER.getSession(rpcResponse.getRequestId());
        if (rpcFuture == null) { // 可能超时已被移除
            return;
        }
        if (rpcFuture instanceof RpcSessionFuture rpcSessionFuture) {
            if (rpcSessionFuture.isSessionFinish()) {
                remove(rpcResponse.getRequestId());
            } else {
                // 过期时间
                Long expireAt = RPC_FUTURE_SESSION_MANAGER.getExpireAt(rpcResponse.getRequestId());
                boolean needFlushForExpired = RpcSessionFlushStrategy.isNeedFlushForExpired(expireAt, rpcSessionFuture.getTimeOut());
                if (needFlushForExpired) {
                    // 自动叠加请求时间
                    RPC_FUTURE_SESSION_MANAGER.flushTime(rpcResponse.getRequestId(), System.currentTimeMillis() + rpcFuture.getTimeOut());
                }
            }
        } else {
            // 清掉
            remove(rpcResponse.getRequestId());
        }
        // complete只会嗲用一次
        rpcFuture.complete(rpcResponse);
        rpcFuture.setResponseTime(System.currentTimeMillis());
        // 执行完结事件
        List<RpcResponseMsgListener> listeners = rpcFuture.getListeners();
        if (listeners != null) {
            for (RpcResponseMsgListener rpcResponseMsgListener : new ArrayList<>(listeners)) {
        VirtualThreadPool.execute(() -> rpcResponseMsgListener.onResponse(rpcResponse));
            }
        }
    }

    public static RpcFuture addRequest(RpcRequest rpcRequest) {
        return addRequest(rpcRequest, NumberConstant.OVER_TIME);
    }

    public static RpcFuture addRequest(RpcRequest rpcRequest, long timeOut) {
        if (rpcRequest == null) {
            return null;
        }
        if (rpcRequest.getRequestId() == null) {
            return null;
        }
        if (timeOut > 0) {
            RpcFuture rpcFuture = new RpcFuture(timeOut);
            rpcFuture.setRequestId(rpcRequest.getRequestId());
            RPC_FUTURE_SESSION_MANAGER.initSession(rpcRequest.getRequestId(), rpcFuture, System.currentTimeMillis() + timeOut);
            return rpcFuture;
        } else {
            throw new RuntimeException("超时时间配置不合法");
        }
    }

    public static RpcFuture remove(String sessionId) {
        return RPC_FUTURE_SESSION_MANAGER.release(sessionId);
    }

    public static int concurrentSize() {
        return RPC_FUTURE_SESSION_MANAGER.sessionSize();
    }

    /**
     * 校验是否存在key
     *
     * @param requestId
     * @return
     */
    public static boolean contains(String requestId) {
        return RPC_FUTURE_SESSION_MANAGER.contains(requestId);
    }

    /**
     * 处理timeout
     *
     * @param future
     */
    private static void handleTimeOut(RpcFuture future) {
        List<RpcResponseMsgListener> listeners = future.getListeners();
        if (listeners != null) {
            for (RpcResponseMsgListener rpcResponseMsgListener : new ArrayList<>(listeners)) {
        VirtualThreadPool.execute(rpcResponseMsgListener::onTimeout);
            }
        }
    }

    public static void flushTime(String sessionId, long expiredAt) {
        RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, expiredAt);
    }

    public static RpcSessionFuture stopSessionGracefully(String sessionId) {
        RpcSessionFuture future = (RpcSessionFuture) RPC_FUTURE_SESSION_MANAGER.getSession(sessionId);
        if (!future.isSessionFinish()) {
            RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, System.currentTimeMillis() + NumberConstant.ONE_POINT_FILE_K);
            future.setSessionFinish(true);
        }
        return future;
    }
}
