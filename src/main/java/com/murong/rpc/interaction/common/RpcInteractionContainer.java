package com.murong.rpc.interaction.common;


import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionProcess;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.handler.RpcResponseMsgListener;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RpcInteractionContainer {

    @Getter
    private static final SessionManager<RpcFuture> RPC_FUTURE_SESSION_MANAGER = new SessionManager<>(NumberConstant.OVER_TIME, RpcInteractionContainer::handleTimeOut);

    /**
     * 校验并刷新session请求时长
     *
     * @param rpcSessionRequest
     */
    static RpcSessionFuture verifySessionRequest(RpcSessionRequest rpcSessionRequest) {
        if (rpcSessionRequest == null) {
            throw new RuntimeException("rpcSessionRequest 不能为null");
        }
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        if (rpcSession == null) {
            throw new RuntimeException("session 标识不能为null");
        }
        if (contains(rpcSession.getSessionId())) {
            if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.START) {
                throw new RuntimeException("不可重复开启会话");
            }
            RpcSessionFuture rpcFuture = getSessionFuture(rpcSession.getSessionId());
            if (!rpcFuture.isSessionFinish()) {
                rpcFuture.setRequestTime(System.currentTimeMillis());
                // 刷新时间
                RPC_FUTURE_SESSION_MANAGER.flushTime(rpcSession.getSessionId(), rpcSession.getTimeOutMillis());
            }
            return rpcFuture;
        } else {
            if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.ING) {
                throw new RuntimeException("会话不存在或已结束,不可以发送会话消息");
            } else if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.FiNISH) {
                throw new RuntimeException("会话不存在或已结束,不可以发送会话消息");
            } else {
                RpcSessionFuture rpcFuture = new RpcSessionFuture(rpcSession.getTimeOutMillis());
                rpcFuture.setRequestId(rpcSession.getSessionId());
                RPC_FUTURE_SESSION_MANAGER.initSession(rpcSession.getSessionId(), rpcFuture, System.currentTimeMillis() + rpcSession.getTimeOutMillis());
                return rpcFuture;
            }
        }

    }

    /**
     * 获取到sessionFuture
     */
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
        // complete只会嗲用一次
        rpcFuture.complete(rpcResponse);
        rpcFuture.setResponseTime(System.currentTimeMillis());
        if (rpcFuture instanceof RpcSessionFuture rpcSessionFuture) {
            if (rpcSessionFuture.isSessionFinish()) {
                remove(rpcResponse.getRequestId());
            } else {
                // 自动叠加请求时间
                RPC_FUTURE_SESSION_MANAGER.flushTime(rpcResponse.getRequestId(), rpcSessionFuture.getTimeOut());
                executeOnResponse(rpcFuture, rpcResponse);
            }
        } else {
            // 清掉
            remove(rpcResponse.getRequestId());
            executeOnResponse(rpcFuture, rpcResponse);
        }
    }

    private static void executeOnResponse(RpcFuture rpcFuture, RpcResponse rpcResponse) {
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
    private static void handleTimeOut(String sessionId, RpcFuture future) {
        if (future == null) {
            return;
        }
        if (future instanceof RpcSessionFuture sessionFuture) {
            if (sessionFuture.isSessionFinish()) {
                return;
            }
        }
        List<RpcResponseMsgListener> listeners = future.getListeners();
        if (listeners != null) {
            for (RpcResponseMsgListener rpcResponseMsgListener : new ArrayList<>(listeners)) {
                VirtualThreadPool.execute(rpcResponseMsgListener::onTimeout);
            }
        }
    }

    public static void flushTime(String sessionId, long sessionTime) {
        RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, sessionTime);
    }

    public static RpcSessionFuture stopSessionGracefully(String sessionId) {
        RpcSessionFuture future = (RpcSessionFuture) RPC_FUTURE_SESSION_MANAGER.getSession(sessionId);
        if (future == null) {
            return null;
        }
        if (!future.isSessionFinish()) {
            future.setSessionFinish(true);
            RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, NumberConstant.ONE_POINT_FILE_K);
        }
        return future;
    }
}
