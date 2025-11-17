package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.RpcReactionWaiter;


/**
 * Interface for handling simple RPC request messages.
 * Implementations of this interface will process incoming RPC requests and optionally send reactions back.
 */
public interface RpcSimpleRequestMsgHandler {
    /**
     * If the request request requires a reaction,
     * the reaction can be set to body and perform the RpcReactionWaiter.write operation
     * if request.isNeedReaction()
     * waiter.sendReaction(reaction);
     * Note that if the operation is particularly time-consuming--- it needs to be handled asynchronously to avoid thread blocking and affect the consumption of other messages
     *
     */
    void onReceiveRequest(final RpcRequest request, final RpcReactionWaiter waiter);
}
