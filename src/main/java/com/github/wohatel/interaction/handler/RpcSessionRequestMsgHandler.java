package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import io.netty.channel.ChannelHandlerContext;

public interface RpcSessionRequestMsgHandler {
    /**
     * Note that once the processing of a message is time-consuming,
     * it will affect the consumption of other messages,
     * so it is recommended to use asynchronous threads to process the read logic
     *
     */
    default boolean sessionStart(ChannelHandlerContext ctx, final RpcSessionContextWrapper contextWrapper) {
        return true;
    }

    /**
     * Note that once the processing of a message is time-consuming,
     * it will affect the consumption of other messages,
     * so it is recommended to use asynchronous threads to process the read logic
     */
    void channelRead(ChannelHandlerContext ctx, final RpcSessionContextWrapper contextWrapper, final RpcSessionRequest request);

    default void sessionStop(ChannelHandlerContext ctx, final RpcSessionContextWrapper contextWrapper) {

    }

}
