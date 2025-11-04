package com.github.wohatel.interaction.proxy;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.util.OneTimeLock;
import com.github.wohatel.util.VirtualThreadPool;

/**
 * The file receiver handles the event interface
 *
 * @author yaochuang 2025/03/28 09:44
 */
public class RpcSessionRequestMsgHandlerExecProxy {

    /**
     * The file is transferred finished
     *
     */
    public static void sessionStop(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler, final RpcSessionContextWrapper rpcSessionContextWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcSessionContextWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcSessionRequestMsgHandler.onSessionStop(rpcSessionContextWrapper)));
    }

    /**
     * The file is transferred finished
     *
     */
    public static void onFinally(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler, final RpcSessionContextWrapper rpcSessionContextWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FINALLY + rpcSessionContextWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcSessionRequestMsgHandler.onFinally(rpcSessionContextWrapper)));
    }
}