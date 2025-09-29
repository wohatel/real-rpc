package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * 普通消息接收事件
 *
 * @author yaochuang 2025/03/25 11:29
 */
public interface RpcSimpleRequestMsgHandler {
    /**
     * If the request request requires a response,
     * the response can be set to body and perform the RpcMsgTransUtil.write operation
     * if request.isNeedResponse()
     * RpcMsgTransManager.write(ctx.channel(), response);
     * Note that if the operation is particularly time-consuming--- it needs to be handled asynchronously to avoid thread blocking and affect the consumption of other messages
     *
     */
    void channelRead(ChannelHandlerContext ctx, final RpcRequest request);
}
