package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.RpcReactionWaiter;

/**
 * 普通消息接收事件
 *
 * @author yaochuang 2025/03/25 11:29
 */
public interface RpcSimpleRequestMsgHandler {
    /**
     * If the request request requires a reaction,
     * the reaction can be set to body and perform the RpcMsgTransUtil.write operation
     * if request.isNeedReaction()
     * waiter.sendReaction(ctx.channel(), reaction);
     * Note that if the operation is particularly time-consuming--- it needs to be handled asynchronously to avoid thread blocking and affect the consumption of other messages
     *
     */
    void onReceiveRequest(final RpcRequest request, final RpcReactionWaiter waiter);
}
