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
 * A proxy class for executing RpcSessionRequestMsgHandler methods with thread safety guarantees.
 * This class ensures that certain operations are only executed once per session using OneTimeLock.
 * It utilizes a virtual thread pool for executing the handler methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSessionRequestMsgHandlerExecProxy {

    /**
     * Executes the sessionStop method of the RpcSessionRequestMsgHandler with thread safety.
     * This method ensures that the sessionStop operation is only performed once per session.
     *
     * @param rpcSessionRequestMsgHandler The RpcSessionRequestMsgHandler instance to execute the sessionStop method on
     * @param rpcSessionContextWrapper The RpcSessionContextWrapper containing the session context
     */
    public static void sessionStop(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler, final RpcSessionContextWrapper rpcSessionContextWrapper, RpcSessionReactionWaiter waiter) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcSessionContextWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcSessionRequestMsgHandler.onSessionStop(rpcSessionContextWrapper, waiter)));
    }


    /**
     * Executes the finally block of a RPC session request handler in a controlled manner.
     * This method ensures that the finally block is executed only once per session and manages thread execution.
     *
     * @param rpcSessionRequestMsgHandler The handler for RPC session request messages
     * @param rpcSessionContextWrapper    The wrapper containing RPC session context information
     * @param waiter                      The waiter object for handling RPC session reactions
     */
    public static void onFinally(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler, final RpcSessionContextWrapper rpcSessionContextWrapper, RpcSessionReactionWaiter waiter) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FINALLY + rpcSessionContextWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcSessionRequestMsgHandler.onFinally(rpcSessionContextWrapper, waiter)));
    }
}