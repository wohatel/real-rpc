package com.github.wohatel.interaction.proxy;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionReactionWaiter;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.util.DefaultVirtualThreadPool;
import com.github.wohatel.util.OneTimeLock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The file receiver handles the event interface
 *
 * @author yaochuang 2025/03/28 09:44
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSessionRequestMsgHandlerExecProxy {

    /**
     * The file is transferred finished
     *
     */
    public static void sessionStop(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler, final RpcSessionContextWrapper rpcSessionContextWrapper, RpcSessionReactionWaiter waiter) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcSessionContextWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcSessionRequestMsgHandler.onSessionStop(rpcSessionContextWrapper, waiter)));
    }

    /**
     * The file is transferred finished
     *
     */
    public static void onFinally(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler, final RpcSessionContextWrapper rpcSessionContextWrapper, RpcSessionReactionWaiter waiter) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FINALLY + rpcSessionContextWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcSessionRequestMsgHandler.onFinally(rpcSessionContextWrapper, waiter)));
    }
}