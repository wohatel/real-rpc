package com.github.wohatel.interaction.common;


import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.util.SessionManager;
import com.github.wohatel.util.VirtualThreadPool;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RpcFutureTransManager {

    @Getter
    private static final SessionManager<RpcFuture> RPC_FUTURE_SESSION_MANAGER = new SessionManager<>(RpcNumberConstant.OVER_TIME, RpcFutureTransManager::handleTimeOut);

    /**
     * Validates and refreshes the session request duration
     *
     */
    public static RpcSessionFuture verifySessionRequest(RpcSessionRequest rpcSessionRequest) {
        if (rpcSessionRequest == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSessionRequest cannot be null");
        }
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        if (rpcSession == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "session the identity cannot be null");
        }
        if (contains(rpcSession.getSessionId())) {
            if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.TOSTART) {
                throw new RpcException(RpcErrorEnum.SEND_MSG, "sessions cannot be opened repeatedly");
            }
            RpcSessionFuture rpcFuture = getSessionFuture(rpcSession.getSessionId());
            RpcSessionProcess rpcSessionProcess = rpcFuture.getRpcSessionProcess();
            if (rpcSessionProcess != RpcSessionProcess.FINISHED) {
                rpcFuture.setRequestTime(System.currentTimeMillis());
                // 刷新时间
                RPC_FUTURE_SESSION_MANAGER.flushTime(rpcSession.getSessionId(), rpcSession.getTimeOutMillis());
            }
            return rpcFuture;
        } else {
            if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.RUNNING) {
                throw new RpcException(RpcErrorEnum.SEND_MSG, "session does not exist or has ended, and session messages cannot be sent");
            } else if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.FINISHED) {
                throw new RpcException(RpcErrorEnum.SEND_MSG, "the session does not exist or has ended, and there is no need to end the session");
            } else {
                RpcSessionFuture rpcFuture = new RpcSessionFuture(rpcSession.getTimeOutMillis());
                rpcFuture.setFutureId(rpcSession.getSessionId());
                rpcFuture.setRpcSessionProcess(RpcSessionProcess.TOSTART);
                RPC_FUTURE_SESSION_MANAGER.initSession(rpcSession.getSessionId(), rpcFuture, System.currentTimeMillis() + rpcSession.getTimeOutMillis());
                return rpcFuture;
            }
        }

    }

    public static RpcSessionFuture getSessionFuture(String sessionId) {
        return (RpcSessionFuture) RPC_FUTURE_SESSION_MANAGER.getSession(sessionId);
    }

    public static void addReaction(RpcReaction rpcReaction) {
        if (rpcReaction == null) {
            return;
        }
        if (rpcReaction.getReactionId() == null) {
            return;
        }
        RpcFuture rpcFuture = RPC_FUTURE_SESSION_MANAGER.getSession(rpcReaction.getReactionId());
        if (rpcFuture == null) {
            return;
        }
        // complete only call once
        rpcFuture.setReactionTime(System.currentTimeMillis());
        if (rpcFuture instanceof RpcSessionFuture rpcSessionFuture) {
            RpcSessionProcess rpcSessionProcess = rpcSessionFuture.getRpcSessionProcess();
            switch (rpcSessionProcess) {
                case TOSTART -> {
                    rpcFuture.complete(rpcReaction);
                }
                case FINISHED -> {
                    remove(rpcReaction.getReactionId());
                }
                case RUNNING -> {
                    RPC_FUTURE_SESSION_MANAGER.flushTime(rpcReaction.getReactionId(), rpcSessionFuture.getTimeOut());
                    executeOnReaction(rpcFuture, rpcReaction);
                }
            }
        } else {
            // 清掉
            remove(rpcReaction.getReactionId());
            executeOnReaction(rpcFuture, rpcReaction);
        }
    }

    private static void executeOnReaction(RpcFuture rpcFuture, RpcReaction rpcReaction) {
        List<RpcReactionMsgListener> listeners = rpcFuture.getListeners();
        if (listeners != null) {
            for (RpcReactionMsgListener rpcReactionMsgListener : new ArrayList<>(listeners)) {
                VirtualThreadPool.execute(() -> rpcReactionMsgListener.onReaction(rpcReaction));
            }
        }
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
            rpcFuture.setFutureId(rpcRequest.getRequestId());
            RPC_FUTURE_SESSION_MANAGER.initSession(rpcRequest.getRequestId(), rpcFuture, System.currentTimeMillis() + timeOut);
            return rpcFuture;
        } else {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the timeout time configuration is not legal");
        }
    }

    public static RpcFuture remove(String sessionId) {
        return RPC_FUTURE_SESSION_MANAGER.release(sessionId);
    }


    public static int concurrentSize() {
        return RPC_FUTURE_SESSION_MANAGER.sessionSize();
    }


    public static boolean contains(String requestId) {
        return RPC_FUTURE_SESSION_MANAGER.contains(requestId);
    }


    private static void handleTimeOut(String sessionId, RpcFuture future) {
        if (future == null) {
            return;
        }
        if (future instanceof RpcSessionFuture sessionFuture) {
            if (sessionFuture.getRpcSessionProcess() == RpcSessionProcess.FINISHED) {
                return;
            }
        }
        List<RpcReactionMsgListener> listeners = future.getListeners();
        if (listeners != null) {
            for (RpcReactionMsgListener rpcReactionMsgListener : new ArrayList<>(listeners)) {
                VirtualThreadPool.execute(rpcReactionMsgListener::onTimeout);
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
        if (future.getRpcSessionProcess() != RpcSessionProcess.FINISHED) {
            future.setRpcSessionProcess(RpcSessionProcess.FINISHED);
            RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, RpcNumberConstant.ONE_POINT_FILE_K);
        }
        return future;
    }
}
